package com.squirrel.index12306.biz.ticketservice.mq.consumer;

import com.alibaba.fastjson2.JSON;
import com.squirrel.index12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import com.squirrel.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.ticketservice.mq.domain.MessageWrapper;
import com.squirrel.index12306.biz.ticketservice.mq.event.DelayCloseOrderEvent;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

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

    private final TicketOrderRemoteService ticketOrderRemoteService;

    @Override
    public void onMessage(MessageWrapper<DelayCloseOrderEvent> delayCloseOrderEventMessageWrapper) {
        log.info("[延迟关闭订单] 开始消费：{}", JSON.toJSONString(delayCloseOrderEventMessageWrapper));
        // 消费消息
        DelayCloseOrderEvent delayCloseOrderEvent = delayCloseOrderEventMessageWrapper.getMessage();
        ticketOrderRemoteService.closeTickOrder(new CancelTicketOrderReqDTO(delayCloseOrderEvent.getOrderSn()));
        // TODO 释放车票余量
        // TODO 修改座位状态
    }
}
