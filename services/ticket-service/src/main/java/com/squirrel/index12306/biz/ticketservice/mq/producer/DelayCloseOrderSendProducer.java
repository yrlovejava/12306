package com.squirrel.index12306.biz.ticketservice.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import com.squirrel.index12306.biz.ticketservice.mq.domain.MessageWrapper;
import com.squirrel.index12306.biz.ticketservice.mq.event.DelayCloseOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 延迟关闭订单生产者
 */
@Slf4j
@Component
public class DelayCloseOrderSendProducer extends AbstractCommonSendProduceTemplate<DelayCloseOrderEvent> {

    private final ConfigurableEnvironment environment;

    public DelayCloseOrderSendProducer(@Autowired RocketMQTemplate rocketMQTemplate,@Autowired ConfigurableEnvironment environment){
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(DelayCloseOrderEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("延迟关闭订单")
                .keys(messageSendEvent.getOrderSn())
                .topic(environment.resolvePlaceholders(TicketRocketMQConstant.TICKET_CREATE_TOPIC_KEY))
                .tag(environment.resolvePlaceholders(TicketRocketMQConstant.TICKET_DELAY_CLOSE_TAG_KEY))
                .sentTimeout(2000L)
                // RocketMQ 延迟消息级别 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                .delayLevel(14)
                .build();
    }

    @Override
    protected Message<?> buildMessage(DelayCloseOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                // 创建消息的主体内容
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                // 设置消息的唯一键
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                // 设置消息的标签
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                // RocketMQ 延迟消息级别 1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
                // 16 代表 30m，为了演示效果所以选择该级别，正常按照需求设置
                .setHeader(MessageConst.PROPERTY_DELAY_TIME_LEVEL, 4)
                .build();
    }
}
