package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.CarriageDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.CarriageMapper;
import com.squirrel.index12306.biz.ticketservice.service.CarriageService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.cache.core.CacheLoader;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_QUERY_CARRIAGE_NUMBER_LIST;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_CARRIAGE;

/**
 * 列车车厢接口层实现
 */
@Service
@RequiredArgsConstructor
public class CarriageServiceImpl implements CarriageService {

    private final DistributedCache distributedCache;
    private final CarriageMapper carriageMapper;
    private final RedissonClient redissonClient;

    /**
     * 查询列车车厢号集合
     *
     * @param trainId      列车 ID
     * @param carriageType 车厢类型
     * @return 车厢号集合
     */
    @Override
    public List<String> listCarriageNumber(String trainId, Integer carriageType) {
        final String key = TRAIN_CARRIAGE + trainId;
        return this.safeGetCarriageNumber(
                key,
                carriageType,
                () -> {
                    // 在数据库中查询对应车厢类型的车厢号
                    List<CarriageDO> carriageDOList = carriageMapper.selectList(Wrappers.lambdaQuery(CarriageDO.class)
                            .eq(CarriageDO::getTrainId, trainId)
                            .eq(CarriageDO::getCarriageType, carriageType)
                    );
                    // 收集所有的车厢号
                    List<String> carriageListWithOnlyNumber = carriageDOList.stream().map(CarriageDO::getCarriageNumber).toList();
                    // 拼接为字符串
                    return StrUtil.join(StrUtil.COMMA,carriageListWithOnlyNumber);
                }
        );
    }

    /**
     * 获取StringRedisTemplate中的哈希操作对象
     * @return HashOperations<String,Object,Object>
     */
    private HashOperations<String,Object,Object> getHashOperations() {
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        return stringRedisTemplate.opsForHash();
    }

    /**
     * 获取车厢号集合字符串
     * @param key redis中key
     * @param carriageType 车厢类型
     * @return 车厢号集合字符串
     */
    private String getCarriageNumberListStr(final String key,Integer carriageType) {
        HashOperations<String, Object, Object> hashOperations = this.getHashOperations();
        Object carriageNumberObj = hashOperations.get(key, String.valueOf(carriageType));
        return Optional.ofNullable(carriageNumberObj).map(Object::toString).orElse("");
    }

    /**
     *
     * @param key key
     * @param carriageType 车厢类型
     * @param loader 缓存加载函数式接口
     * @return 车厢号集合
     */
    private List<String> safeGetCarriageNumber(final String key, Integer carriageType, CacheLoader<String> loader){
        // 获取车厢号
        String result = this.getCarriageNumberListStr(key, carriageType);
        if(!CacheUtil.isNullOrBlank(result)){
            return StrUtil.split(result,StrUtil.COMMA);
        }

        // 如果没有
        // 获取分布式锁
        RLock lock = redissonClient.getLock(String.format(LOCK_QUERY_CARRIAGE_NUMBER_LIST, key));
        lock.lock();
        try {
            // 双重检索
            if(CacheUtil.isNullOrBlank(result = this.getCarriageNumberListStr(key,carriageType))) {
                // 如果从数据库中查询并加载缓存后还是没有
                if (CacheUtil.isNullOrBlank(result = this.loadAndSet(carriageType,key,loader))) {
                    return Collections.emptyList();
                }
            }
        }finally {
            lock.unlock();
        }
        return StrUtil.split(result,StrUtil.COMMA);
    }

    /**
     * 加载缓存
     * @param carriageType 车厢类型
     * @param key key
     * @param loader 缓存加载函数式接口
     * @return 车厢号集合字符串
     */
    private String loadAndSet(Integer carriageType,final String key,CacheLoader<String> loader) {
        // 调用者自定义的加载缓存逻辑
        String result = loader.load();
        if(CacheUtil.isNullOrBlank(result)){
            return result;
        }
        // 写入缓存
        HashOperations<String, Object, Object> hashOperations = this.getHashOperations();
        hashOperations.putIfAbsent(key,String.valueOf(carriageType),result);
        return result;
    }
}
