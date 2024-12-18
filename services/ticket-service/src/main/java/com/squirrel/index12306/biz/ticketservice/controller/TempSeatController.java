package com.squirrel.index12306.biz.ticketservice.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO 待删除，联调临时解决方案
 */
@Deprecated
@RestController
@RequiredArgsConstructor
public class TempSeatController {

    private final SeatMapper seatMapper;

    /**
     * 座位重置
     */
    @PostMapping("/api/ticket-service/temp/seat/reset")
    public Result<Void> purchaseTickets(@RequestParam String trainId) {
        SeatDO seatDO = new SeatDO();
        seatDO.setTrainId(Long.parseLong(trainId));
        seatDO.setSeatStatus(0);
        seatMapper.update(seatDO, Wrappers.lambdaUpdate());
        return Results.success();
    }
}
