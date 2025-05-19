package com.squirrel.index12306.biz.ticketservice.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.ThreadUtil;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * TODO 待删除，联调临时解决方案
 */
@Deprecated
@RestController
@RequiredArgsConstructor
public class TempSeatController {

    private final SeatMapper seatMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    /**
     * 座位重置
     */
    @PostMapping("/api/ticket-service/temp/seat/reset")
    public Result<Void> purchaseTickets(@RequestParam String trainId) {
        SeatDO seatDO = new SeatDO();
        seatDO.setTrainId(Long.parseLong(trainId));
        seatDO.setSeatStatus(SeatStatusEnum.AVAILABLE.getCode());
        seatMapper.update(seatDO, Wrappers.lambdaUpdate(SeatDO.class).eq(SeatDO::getTrainId, trainId));
        ThreadUtil.sleep(5000);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 查询列车站点关系
        List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(Wrappers.lambdaQuery(TrainStationRelationDO.class)
                .eq(TrainStationRelationDO::getTrainId, trainId));
        // 删除redis中的各种车厢类型的余票
        for (TrainStationRelationDO each : trainStationRelationDOList) {
            // 获取key后缀
            String keySuffix = StrUtil.join("_", each.getTrainId(), each.getDeparture(), each.getArrival());
            stringRedisTemplate.delete(TRAIN_STATION_REMAINING_TICKET + keySuffix);
        }
        stringRedisTemplate.delete(TICKET_AVAILABILITY_TOKEN_BUCKET + trainId);
        return Results.success();
    }
}
