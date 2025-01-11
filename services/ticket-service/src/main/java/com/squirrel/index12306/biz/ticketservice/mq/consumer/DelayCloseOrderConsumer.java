package com.squirrel.index12306.biz.ticketservice.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.squirrel.index12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import com.squirrel.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.ticketservice.mq.domain.MessageWrapper;
import com.squirrel.index12306.biz.ticketservice.mq.event.DelayCloseOrderEvent;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 延迟关闭订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.TICKET_CREATE_TOPIC_KEY,// 订阅的主题
        selectorExpression = TicketRocketMQConstant.TICKET_DELAY_CLOSE_TAG_KEY,// 消息标签过滤规则
        consumerGroup = TicketRocketMQConstant.TICKET_DELAY_CLOSE_CG_KEY)// 消费者组
public final class DelayCloseOrderConsumer implements RocketMQListener<MessageWrapper<DelayCloseOrderEvent>> {

    private final SeatService seatService;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final DistributedCache distributedCache;

    @Override
    public void onMessage(MessageWrapper<DelayCloseOrderEvent> delayCloseOrderEventMessageWrapper) {
        log.info("[延迟关闭订单] 开始消费：{}", JSON.toJSONString(delayCloseOrderEventMessageWrapper));
        // 消费消息
        DelayCloseOrderEvent delayCloseOrderEvent = delayCloseOrderEventMessageWrapper.getMessage();
        ticketOrderRemoteService.closeTickOrder(new CancelTicketOrderReqDTO(delayCloseOrderEvent.getOrderSn()));
        String trainId = delayCloseOrderEvent.getTrainId();
        String departure = delayCloseOrderEvent.getDeparture();
        String arrival = delayCloseOrderEvent.getArrival();
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = delayCloseOrderEvent.getTrainPurchaseTicketResults();
        // 释放车票
        seatService.unlock(trainId,departure,arrival,trainPurchaseTicketResults);
        // 根据座位类型分类
        Map<Integer, List<TrainPurchaseTicketRespDTO>> seatTypeMap = trainPurchaseTicketResults.stream()
                .collect(Collectors.groupingBy(TrainPurchaseTicketRespDTO::getSeatType));
        // 获取key后缀
        String keySuffix = StrUtil.join("_",trainId,departure,arrival);
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        seatTypeMap.forEach(
                (seatType,passengerSeatDetails) -> stringRedisTemplate.opsForHash()
                        .increment(TRAIN_STATION_REMAINING_TICKET + keySuffix,String.valueOf(seatType),passengerSeatDetails.size())
        );
    }
}
