package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant;
import com.squirrel.index12306.biz.ticketservice.common.enums.RegionStationQueryTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.RegionDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.StationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.StationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.RegionStationService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.cache.core.CacheLoader;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import com.squirrel.index12306.framework.starter.common.enums.FlagEnum;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.*;

/**
 * 地区以及车站接口实现层
 */
@Service
@RequiredArgsConstructor
public class RegionStationServiceImpl implements RegionStationService {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&城市查询参数
     * @return 车站&城市站点集合信息
     */
    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam) {
        String key;
        if (StrUtil.isNotBlank(requestParam.getName())) {
            // 先从redis中查询
            key = REGION_STATION + requestParam.getName();
            return this.safeGetRegionStation(
                    key ,
                    () -> {
                        // 构造查询参数
                        LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                                .likeRight(StationDO::getName, requestParam.getName())// 右模糊匹配名字
                                .or()
                                .likeRight(StationDO::getSpell, requestParam.getName());// 右模糊匹配拼音
                        List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);
                        return JSON.toJSONString(BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class));
                    },
                    requestParam.getName()
            );
        }
        key = REGION_STATION + requestParam.getQueryType();
        return this.safeGetRegionStation(
                key,
                () -> {
                    // 构造查询条件
                    LambdaQueryWrapper<RegionDO> queryWrapper = switch (requestParam.getQueryType()) {
                        case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                                .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
                        case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                                .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
                        case 2 -> Wrappers.lambdaQuery(RegionDO.class)
                                .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
                        case 3 -> Wrappers.lambdaQuery(RegionDO.class)
                                .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
                        case 4 -> Wrappers.lambdaQuery(RegionDO.class)
                                .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
                        case 5 -> Wrappers.lambdaQuery(RegionDO.class)
                                .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
                        default -> throw new ClientException("查询失败，请检查查询参数是否正确");
                    };
                    // 查询所有的地区
                    List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);
                    return JSON.toJSONString(BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class));
                },
                String.valueOf(requestParam.getQueryType())
        );
    }

    /**
     * 从缓存中获取地区数据，如果没有自定义加载策略
     * @param key key
     * @param loader 自定义的记载策略
     * @param param 参数
     * @return 地区数据
     */
    private List<RegionStationQueryRespDTO> safeGetRegionStation(final String key, CacheLoader<String> loader,String param){
        List<RegionStationQueryRespDTO> result;
        // 如果缓存中有直接返回
        if(CollUtil.isNotEmpty(result = JSON.parseArray(distributedCache.get(key,String.class), RegionStationQueryRespDTO.class))) {
            return result;
        }

        // 如果缓存中没有
        // 获取分布式锁
        String lockKey = String.format(LOCK_QUERY_REGION_STATION_LIST,param);
        RLock lock = redissonClient.getLock(lockKey);
        try {
            // 双重检索
            if(CollUtil.isEmpty(result = JSON.parseArray(distributedCache.get(key,String.class),RegionStationQueryRespDTO.class))) {
                // 如果还是没有，使用自定义的加载策略
                if(CollUtil.isEmpty(result = this.loadAndSet(key,loader))){
                    // 如果自定义加载策略也没有，那么返回空集合
                    return Collections.emptyList();
                }
            }
        }finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 加载缓存
     * @param key key
     * @param loader 自定义加载策略
     * @return 记载的数据
     */
    private List<RegionStationQueryRespDTO> loadAndSet(final String key, CacheLoader<String> loader) {
        // 使用自定义的加载策略
        String result = loader.load();
        // 检查是否存在
        if (CacheUtil.isNullOrBlank(result)) {
            return Collections.emptyList();
        }
        // 转换为json格式
        List<RegionStationQueryRespDTO> respDTOList = JSON.parseArray(result, RegionStationQueryRespDTO.class);
        // 加载到缓存中去
        distributedCache.put(
                key,
                result,
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        // 返回数据
        return respDTOList;
    }

    /**
     * 查询所有车站&城市站点集合信息
     *
     * @return 车站返回数据集合
     */
    @Override
    public List<StationQueryRespDTO> listAllStation() {
        return distributedCache.get(
                STATION_ALL,
                List.class,
                () -> BeanUtil.convert(stationMapper.selectList(Wrappers.emptyWrapper()),StationQueryRespDTO.class),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
    }
}
