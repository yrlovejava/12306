package com.squirrel.index12306.biz.ticketservice.cannal;

import cn.hutool.core.collection.CollUtil;
import com.squirrel.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.squirrel.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.TicketAvailabilityTokenBucket;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 订单表变更-订单关闭或取消后置处理组件
 * 1.解锁锁定的座位
 * 2.增加令牌桶中令牌的数量
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent,Void> {

    private final TicketAvailabilityTokenBucket ticketAvailabilityTokenBucket;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final SeatService seatService;

    @Override
    public void execute(CanalBinlogEvent message) {
        // 过滤掉非订单关闭或取消的事件
        List<Map<String,Object>> messageDataList = message.getData().stream()
                .filter(each -> each.get("status") != null)
                .filter(each -> Objects.equals(each.get("status"),"30"))
                .toList();
        if(CollUtil.isEmpty(messageDataList)){
            return;
        }
        for (Map<String, Object> each : messageDataList) {
            // 查询订单详情
            Result<TicketOrderDetailRespDTO> orderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(each.get("order_sn").toString());
            TicketOrderDetailRespDTO orderDetailResultData = orderDetailResult.getData();
            if(orderDetailResult.isSuccess() && orderDetailResultData != null){
                String trainId = String.valueOf(orderDetailResultData.getTrainId());
                // 乘车人信息
                List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailResultData.getPassengerDetails();
                // 解锁锁定的座位
                List<TrainPurchaseTicketRespDTO> purchaseRespDTOList = BeanUtil.convert(passengerDetails, TrainPurchaseTicketRespDTO.class);
                seatService.unlock(trainId,orderDetailResultData.getDeparture(),orderDetailResultData.getArrival(),purchaseRespDTOList);

                // 回滚列车余票令牌
                ticketAvailabilityTokenBucket.rollbackInBucket(orderDetailResultData);
            }
        }
    }

    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getActualTable();
    }

    @Override
    public String patternMatchMark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getPatternMatchTable();
    }
}
