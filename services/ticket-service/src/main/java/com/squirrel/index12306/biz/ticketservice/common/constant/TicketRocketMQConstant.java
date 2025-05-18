package com.squirrel.index12306.biz.ticketservice.common.constant;

/**
 * RocketMQ 购票服务常量类
 */
public final class TicketRocketMQConstant {

    /**
     * 购票服务创建相关业务 Topic Key
     */
    public static final String TICKET_CREATE_TOPIC_KEY = "index12306_ticket-service_topic-dev";

    /**
     * 购票服务创建订单后延时关闭业务 Tag Key
     */
    public static final String TICKET_DELAY_CLOSE_TAG_KEY = "index12306_ticket-service_delay-close-order_tag";

    /**
     * 购票服务创建订单后延时关闭业务消费者组 Key
     */
    public static final String TICKET_DELAY_CLOSE_CG_KEY = "index12306_ticket-service_delay-close-order_cg";

    /**
     * Canal 监听数据库余票变更 Topic Key
     */
    public static final String CANAL_COMMON_SYNC_TOPIC_KEY = "index12306_canal_ticket-service_common-sync_topic";

    /**
     * Canal 监听数据库余票变更业务消费者组 Key
     */
    public static final String CANAL_SYNC_COMMON_CG_KEY = "index12306_canal_ticket-service_common-sync_cg";
}
