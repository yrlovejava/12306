package com.squirrel.index12306.biz.orderservice.common.constant;

/**
 * RocketMQ 订单服务常量类
 */
public final class OrderRocketMQConstant {

    /**
     * 支付服务相关业务 Topic Key
     */
    public static final String PAY_GLOBAL_TOPIC_KEY = "index12306_pay-service_topic";

    /**
     * 支付结果回调订单 Tag Key
     */
    public static final String PAY_RESULT_CALLBACK_ORDER_TAG_KEY = "index12306_pay-service_pay-result-callback-order_tag";

    /**
     * 支付结果回调订单消费者组 Key
     */
    public static final String PAY_RESULT_CALLBACK_ORDER_CG_KEY = "index12306_pay-service_pay-result-callback-order_cg";
}