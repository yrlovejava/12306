package com.squirrel.index12306.biz.ticketservice.service.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private final TrainStationService trainStationService;

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
        Map<String, Map<String, String>> trainStationRemainingTicketMaps = new LinkedHashMap<>();
        // 获取key后缀
        String keySuffix = CacheUtil.buildKey(trainId, departure, arrival);
        // 获取分布式锁
        RLock lock = redissonClient.getLock(String.format(LOCK_SAFE_LOAD_SEAT_MARGIN_GET, keySuffix));
        lock.lock();
        try {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            // 获取对应座位类型的余票
            Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
            // 如果缓存没有
            if (CacheUtil.isNullOrBlank(quantityObj)) {
                // 查询列车信息
                TrainDO trainDO = trainMapper.selectById(trainId);
                // 查询路线信息
                List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(trainId, departure, arrival);
                switch (trainDO.getTrainType()) {
                    case 0 -> {
                        for (RouteDTO routeDTO : routeDTOList) {
                            Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                            trainStationRemainingTicket.put("0", selectSeatMargin(trainId, 0, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("1", selectSeatMargin(trainId, 1, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("2", selectSeatMargin(trainId, 2, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            String actualKeySuffix = CacheUtil.buildKey(trainId, routeDTO.getStartStation(), routeDTO.getEndStation());
                            trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                        }
                    }
                    case 1 -> {
                        for (RouteDTO routeDTO : routeDTOList) {
                            Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                            trainStationRemainingTicket.put("3", selectSeatMargin(trainId, 3, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("4", selectSeatMargin(trainId, 4, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("5", selectSeatMargin(trainId, 5, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            String actualKeySuffix = CacheUtil.buildKey(trainId, routeDTO.getStartStation(), routeDTO.getEndStation());
                            trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                        }
                    }
                    case 2 -> {
                        for (RouteDTO routeDTO : routeDTOList) {
                            Map<String, String> trainStationRemainingTicket = new LinkedHashMap<>();
                            trainStationRemainingTicket.put("6", selectSeatMargin(trainId, 6, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("7", selectSeatMargin(trainId, 7, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("8", selectSeatMargin(trainId, 8, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            trainStationRemainingTicket.put("13", selectSeatMargin(trainId, 13, routeDTO.getStartStation(), routeDTO.getStartStation()));
                            String actualKeySuffix = CacheUtil.buildKey(trainId, routeDTO.getStartStation(), routeDTO.getEndStation());
                            trainStationRemainingTicketMaps.put(TRAIN_STATION_REMAINING_TICKET + actualKeySuffix, trainStationRemainingTicket);
                        }
                    }
                }
                // 保存数据到redis中 TODO Lua 脚本执行
                trainStationRemainingTicketMaps.forEach(
                        (cacheKey,chacheMap) -> stringRedisTemplate.opsForHash().putAll(cacheKey, chacheMap)
                );
            }
        } finally {
            lock.unlock();
        }
        return Optional
                .ofNullable(trainStationRemainingTicketMaps.get(TRAIN_STATION_REMAINING_TICKET + keySuffix))
                .orElse(new LinkedHashMap<>());
    }

    /**
     * 查询座位的数量
     *
     * @param trainId   列车id
     * @param type      座位类型
     * @param departure 起始站
     * @param arrival   终点站
     * @return 座位数量(字符串)
     */
    private String selectSeatMargin(String trainId, Integer type, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getSeatType, type)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival);
        return Optional
                .ofNullable(seatMapper.selectCount(queryWrapper))
                .map(String::valueOf)
                .orElse("0");
    }
}
