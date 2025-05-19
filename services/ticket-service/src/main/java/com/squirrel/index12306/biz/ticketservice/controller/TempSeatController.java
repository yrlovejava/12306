package com.squirrel.index12306.biz.ticketservice.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.CarriageDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.CarriageMapper;
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
    private final CarriageMapper carriageMapper;
    private final DistributedCache distributedCache;

    /**
     * 座位重置
     */
    @PostMapping("/api/ticket-service/temp/seat/reset")
    public Result<Void> purchaseTickets(@RequestParam String trainId) {
        SeatDO seatDO = new SeatDO();
        seatDO.setTrainId(Long.parseLong(trainId));
        seatDO.setSeatStatus(SeatStatusEnum.AVAILABLE.getCode());
        seatMapper.update(seatDO, Wrappers.lambdaUpdate());
        ThreadUtil.sleep(5000);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 查询列车站点关系
        List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(Wrappers.lambdaQuery(TrainStationRelationDO.class)
                .eq(TrainStationRelationDO::getTrainId, trainId));
        // 查询不同车厢类型
        List<CarriageDO> carriageDOList = carriageMapper.selectList(Wrappers.lambdaQuery(CarriageDO.class)
                .eq(CarriageDO::getTrainId, trainId)
                .groupBy(CarriageDO::getCarriageType)
                .select(CarriageDO::getCarriageType)
        );
        // 恢复redis中的各种车厢类型的余票
        for (TrainStationRelationDO each : trainStationRelationDOList) {
            // 获取key后缀
            String keySuffix = StrUtil.join("_",each.getTrainId(),each.getDeparture(),each.getArrival());
            for (CarriageDO item : carriageDOList) {
                QueryWrapper<CarriageDO> wrapper = new QueryWrapper<>();
                wrapper.select("sum(seat_count) as seatCount");
                wrapper.eq("carriage_type",item.getCarriageType());
                wrapper.eq("train_id",trainId);
                CarriageDO carriageDO = carriageMapper.selectOne(wrapper);
                stringRedisTemplate.opsForHash().put(TRAIN_STATION_REMAINING_TICKET + keySuffix,
                        String.valueOf(item.getCarriageType()),
                        String.valueOf(carriageDO.getSeatCount()));
            }
        }
        stringRedisTemplate.delete(TICKET_AVAILABILITY_TOKEN_BUCKET + trainId);
        return Results.success();
    }
}
