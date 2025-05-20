package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Maps;
import com.squirrel.index12306.biz.ticketservice.dao.entity.RegionDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_REGION_LIST;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.QUERY_ALL_REGION_LIST;

/**
 * 购票流程过滤器之验证乘客是否重复购买
 */
@Component
@RequiredArgsConstructor
public class TrainTicketQueryParamVerifyChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {

    private final RegionMapper regionMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 标识缓存是否被初始化
     * 默认没被初始化
     */
    private static boolean FLAG = false;

    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        if (requestParam.getDepartureDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().isBefore(LocalDate.now())){
            throw new ClientException("出发日期不能小于当前日期");
        }
        // 验证出发地和目的地是否存在
        // 查找所有的地区
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        HashOperations<String, Object, Object> hashOperations = stringRedisTemplate.opsForHash();
        List<Object> actualExistList = hashOperations.multiGet(
                QUERY_ALL_REGION_LIST,
                ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
        );

        // 计算缓存不存在的区域
        long emptyCount = actualExistList.stream().filter(Objects::isNull).count();
        if (emptyCount == 0L){
            // 都存在直接返回
            return;
        }
        // 如果两个地区都不存在并且全局标志FLAG为true(表示缓存已经被初始化)但是redis缓存键不存在
        // 或者只存在一个地区
        if((emptyCount == 2L && FLAG && !distributedCache.hasKey(QUERY_ALL_REGION_LIST))
        || emptyCount == 1L){
            throw new ClientException("出发地或目的地不存在");
        }

        // 获取分布式锁
        RLock lock = redissonClient.getLock(LOCK_QUERY_REGION_LIST);
        lock.lock();
        try {
            // 双重检索机制
            if (distributedCache.hasKey(QUERY_ALL_REGION_LIST)) {
                actualExistList = hashOperations.multiGet(
                        QUERY_ALL_REGION_LIST,
                        ListUtil.toList(requestParam.getFromStation(), requestParam.getToStation())
                );
                emptyCount = actualExistList.stream().filter(Objects::isNull).count();
                if (emptyCount != 2L) {
                    throw new ClientException("出发地或目的地不存在");
                }
                return;
            }

            // 代码到了这里表示确实没有，那么就从数据库中查询数据
            // 查询所有的地区
            List<RegionDO> regionDOList = regionMapper.selectList(Wrappers.emptyWrapper());
            HashMap<Object, Object> regionValueMap = Maps.newHashMap();
            for (RegionDO each : regionDOList){
                regionValueMap.put(each.getCode(),each.getName());
            }
            // 加载缓存
            hashOperations.putAll(QUERY_ALL_REGION_LIST,regionValueMap);
            // 标识缓存已经被初始化
            FLAG = true;

            // 再次从缓存中查询，如果仍然没有，代表数据库中也没有，那么这个地区确实不存在
            actualExistList = hashOperations.multiGet(
                    QUERY_ALL_REGION_LIST,
                    ListUtil.toList(requestParam.getFromStation(),requestParam.getToStation())
            );
            emptyCount = actualExistList.stream().filter(Objects::isNull).count();
            if (emptyCount != 0L) {
                throw new ClientException("出发地或目的地不存在");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
