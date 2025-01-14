package com.squirrel.index12306.biz.ticketservice.service.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.LOCK_SAFE_LOAD_SEAT_MARGIN_GET;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 座位余量缓存加载
 */
@Component
@RequiredArgsConstructor
public class SeatMarginCacheLoader {

    private final TrainMapper trainMapper;
    private final SeatMapper seatMapper;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;

    /**
     * 加载座位余量缓存
     *
     * @param trainId   列车id
     * @param seatType  座位类型
     * @param departure 起点
     * @param arrival   终点
     * @return 座位类型对应的票的数量
     */
    public Map<String, String> load(String trainId, String seatType, String departure, String arrival) {
        // 剩余车票的map
        Map<String,String> trainStationRemainingTicket = new HashMap<>();
        // 获取key后缀
        String keySuffix = CacheUtil.buildKey(trainId,departure,arrival);
        // 获取分布式锁
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET,keySuffix));
        lock.lock();
        try {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            // 获取对应座位类型的余票
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            // 如果缓存没有
            if (CacheUtil.isNullOrBlank(quantityObj)) {
                // 从数据库中查询
                TrainDO trainDO = trainMapper.selectById(trainId);
                switch (trainDO.getTrainType()){
                    case 0 -> {
                        trainStationRemainingTicket.put("0", selectSeatMargin(trainId, 0, departure, arrival));
                        trainStationRemainingTicket.put("1", selectSeatMargin(trainId, 1, departure, arrival));
                        trainStationRemainingTicket.put("2", selectSeatMargin(trainId, 2, departure, arrival));
                    }
                    case 1 -> {
                        trainStationRemainingTicket.put("3", selectSeatMargin(trainId, 3, departure, arrival));
                        trainStationRemainingTicket.put("4", selectSeatMargin(trainId, 4, departure, arrival));
                        trainStationRemainingTicket.put("5", selectSeatMargin(trainId, 5, departure, arrival));
                        trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, departure, arrival));
                    }
                    case 2 -> {
                        trainStationRemainingTicket.put("6", selectSeatMargin(trainId, 6, departure, arrival));
                        trainStationRemainingTicket.put("7", selectSeatMargin(trainId, 7, departure, arrival));
                        trainStationRemainingTicket.put("8", selectSeatMargin(trainId, 8, departure, arrival));
                        trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, departure, arrival));
                    }
                }
                // 保存数据到redis中
                String buildCacheKey = TRAIN_STATION_REMAINING_TICKET + keySuffix;
                stringRedisTemplate.opsForHash().putAll(buildCacheKey, trainStationRemainingTicket);
                stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
            }
        }finally {
            lock.unlock();
        }
        return trainStationRemainingTicket;
    }

    /**
     * 查询座位的数量
     * @param trainId 列车id
     * @param type 座位类型
     * @param departure 起始站
     * @param arrival 终点站
     * @return 座位数量(字符串)
     */
    private String selectSeatMargin(String trainId,Integer type,String departure,String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        return Optional.ofNullable(seatMapper.selectCount(queryWrapper)).map(String::valueOf).orElse("0");
    }
}
