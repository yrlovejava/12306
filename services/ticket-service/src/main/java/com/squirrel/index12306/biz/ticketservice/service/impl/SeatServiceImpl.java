package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_CARRIAGE_REMAINING_TICKET;

/**
 * 座位接口层实现
 */
@Service
@RequiredArgsConstructor
public class SeatServiceImpl extends ServiceImpl<SeatMapper, SeatDO> implements SeatService {

    private final SeatMapper seatMapper;
    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;

    /**
     * 获取列车车厢中可用的座位集合
     *
     * @param trainId        列车 ID
     * @param carriageNumber 车厢号
     * @param seatType       座位类型
     * @param departure      出发站
     * @param arrival        到达站
     * @return 可用座位集合
     */
    @Override
    public List<String> listAvailableSeat(String trainId, String carriageNumber, Integer seatType, String departure, String arrival) {
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId, trainId)
                .eq(SeatDO::getCarriageNumber, carriageNumber)
                .eq(SeatDO::getSeatType, seatType)
                .eq(SeatDO::getStartStation, departure)
                .eq(SeatDO::getEndStation, arrival)
                .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                .select(SeatDO::getSeatNumber);
        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);
        return seatDOList.stream().map(SeatDO::getSeatNumber).collect(Collectors.toList());
    }

    /**
     * 获取列车车厢余票集合
     *
     * @param trainId           列车 ID
     * @param departure         出发站
     * @param arrival           到达站
     * @param trainCarriageList 车厢编号集合
     * @return 车厢余票集合
     */
    @Override
    public List<Integer> listSeatRemainingTicket(String trainId, String departure, String arrival, List<String> trainCarriageList) {
        // Redis中列车车厢余票key后缀
        String keySuffix = StrUtil.join("_", trainId, departure, arrival);
        if (distributedCache.hasKey(TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix)) {
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            List<Object> trainStationCarriageRemainingTicket = stringRedisTemplate.opsForHash()
                    .multiGet(TRAIN_STATION_CARRIAGE_REMAINING_TICKET + keySuffix, Arrays.asList(trainCarriageList.toArray()));
            if (CollUtil.isNotEmpty(trainStationCarriageRemainingTicket)) {
                return trainStationCarriageRemainingTicket.stream()
                        .map(each -> Integer.parseInt(each.toString()))
                        .collect(Collectors.toList());
            }
        }
        SeatDO seatDO = SeatDO.builder()
                .trainId(Long.parseLong(trainId))
                .startStation(departure)
                .endStation(arrival)
                .build();
        return seatMapper.listSeatRemainingTicket(seatDO, trainCarriageList);
    }

    /**
     * 查询列车有余票的车厢号集合
     *
     * @param trainId      列车 ID
     * @param carriageType 车厢类型
     * @param departure    出发站
     * @param arrival      到达站
     * @return 车厢号集合
     */
    @Override
    public List<String> listUsableCarriageNumber(String trainId, Integer carriageType, String departure, String arrival) {
        // 构造查询条件
        LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                .eq(SeatDO::getTrainId,trainId) // 列车id
                .eq(SeatDO::getSeatType,carriageType) // 座位类型
                .eq(SeatDO::getStartStation,departure) // 起始站
                .eq(SeatDO::getEndStation,arrival) // 终点站
                .eq(SeatDO::getSeatStatus,SeatStatusEnum.AVAILABLE.getCode()) // 座位状态为可选
                .groupBy(SeatDO::getCarriageNumber) // 根据车厢号分组
                .select(SeatDO::getCarriageNumber); // 只查找车厢号
        // 在数据库中查询
        List<SeatDO> seatDOList = seatMapper.selectList(queryWrapper);
        return seatDOList.stream().map(SeatDO::getCarriageNumber).toList();
    }

    /**
     * 锁定选中以及沿途车票状态
     *
     * @param trainId                     列车 ID
     * @param departure                   出发站
     * @param arrival                     到达站
     * @param trainPurchaseTicketRespList 乘车人以及座位信息
     */
    @Override
    public void lockSeat(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketRespList) {
        // 计算所有的路线
        List<RouteDTO> routeList = trainStationService.listTrainStationRoute(trainId, departure, arrival);
        // 锁定座位车票库存
        trainPurchaseTicketRespList.forEach(each -> routeList.forEach(item -> {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, trainId)// 列车id
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())// 车厢号
                    .eq(SeatDO::getStartStation, item.getStartStation())// 路线的开始站点
                    .eq(SeatDO::getEndStation, item.getEndStation()) // 路线的到达站点
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber()); // 座位号
            SeatDO updateSeatDO = SeatDO.builder().seatStatus(SeatStatusEnum.LOCKED.getCode()).build();
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
    }

    /**
     * 解锁选中以及沿途车票状态
     *
     * @param trainId                    列车 ID
     * @param departure                  出发站
     * @param arrival                    到达站
     * @param trainPurchaseTicketResults 乘车人以及座位信息
     */
    @Override
    public void unlock(String trainId, String departure, String arrival, List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults) {
        // 计算所有的路线
        List<RouteDTO> routeList = trainStationService.listTrainStationRoute(trainId, departure, arrival);
        // 释放座位车票库存
        trainPurchaseTicketResults.forEach(each -> routeList.forEach(item -> {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, trainId)// 列车id
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())// 车厢号
                    .eq(SeatDO::getStartStation, item.getStartStation())// 路线的开始站点
                    .eq(SeatDO::getEndStation, item.getEndStation()) // 路线的到达站点
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber()); // 座位号
            SeatDO updateSeatDO = SeatDO.builder().seatStatus(SeatStatusEnum.AVAILABLE.getCode()).build();
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
    }
}
