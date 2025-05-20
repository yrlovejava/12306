package com.squirrel.index12306.biz.payservice.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.payservice.common.constant.PayRocketMQConstant;
import com.squirrel.index12306.biz.payservice.mq.domain.MessageWrapper;
import com.squirrel.index12306.biz.payservice.mq.event.PayResultCallbackOrderEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 支付结果回调订单生成者
 */
@Slf4j
@Component
public class PayResultCallbackOrderSendProduce extends AbstractCommonSendProducerTemplate<PayResultCallbackOrderEvent> {

    public PayResultCallbackOrderSendProduce(@Autowired RocketMQTemplate rocketMQTemplate) {
        super(rocketMQTemplate);
    }

    /**
     * 构建消息发送事件基础扩充属性实体
     *
     * @param messageSendEvent 消息发送事件
     * @return 扩充属性实体
     */
    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(PayResultCallbackOrderEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("支付回调订单")
                .keys(messageSendEvent.getOrderSn())
                .topic(PayRocketMQConstant.PAY_GLOBAL_TOPIC_KEY)
                .tag(PayRocketMQConstant.PAY_RESULT_CALLBACK_TAG_KEY)
                .sentTimeout(2000L)
                .build();
    }

    /**
     * 构建消息基本参数，请求头、Keys...
     *
     * @param messageSendEvent 消息发送事件
     * @param requestParam     扩充属性实体
     * @return 消息基本参数
     */
    @Override
    protected Message<?> buildMessage(PayResultCallbackOrderEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        return MessageBuilder
                .withPayload(new MessageWrapper(requestParam.getKeys(), messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
    }
}
