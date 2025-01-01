package com.squirrel.index12306.biz.orderservice.mq.consumer;

import com.squirrel.index12306.biz.orderservice.common.constant.OrderRocketMQConstant;
import com.squirrel.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import com.squirrel.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import com.squirrel.index12306.biz.orderservice.mq.domain.MessageWrapper;
import com.squirrel.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.squirrel.index12306.biz.orderservice.service.OrderService;
import com.squirrel.index12306.framework.starter.idempotent.annotation.Idempotent;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 支付结果回调订单消费者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = OrderRocketMQConstant.PAY_GLOBAL_TOPIC_KEY,
        selectorExpression = OrderRocketMQConstant.PAY_RESULT_CALLBACK_ORDER_TAG_KEY,
        consumerGroup = OrderRocketMQConstant.PAY_RESULT_CALLBACK_ORDER_CG_KEY
)
public class PayResultCallbackOrderConsumer implements RocketMQListener<MessageWrapper<PayResultCallbackOrderEvent>> {

    private final OrderService orderService;

    @Idempotent(
            uniqueKeyPrefix = "index12306-order:pay_result_callback:",// 唯一键的前缀
            key = "#message.getKeys()+'_'+#message.hashCode()",// 生成key的SPEL表达式
            type = IdempotentTypeEnum.SPEL,// 表达式类型
            scene = IdempotentSceneEnum.MQ,// 幂等场景，MQ
            keyTimeout = 7200L// key有效时间
    )
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void onMessage(MessageWrapper<PayResultCallbackOrderEvent> message) {
        PayResultCallbackOrderEvent payResultCallbackOrderEvent = message.getMessage();
        OrderStatusReversalDTO orderStatusReversalDTO = OrderStatusReversalDTO.builder()
                .orderSn(payResultCallbackOrderEvent.getOrderSn())
                .orderStatus(OrderStatusEnum.ALREADY_PAID.getStatus())
                .build();
        // 修改订单状态
        orderService.statusReversal(orderStatusReversalDTO);
        // 支付结果回调
        orderService.payCallbackOrder(payResultCallbackOrderEvent);
    }
}
