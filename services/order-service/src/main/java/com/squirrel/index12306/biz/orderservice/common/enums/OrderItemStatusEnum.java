package com.squirrel.index12306.biz.orderservice.common.enums;

import cn.crane4j.annotation.ContainerEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单明细状态枚举
 */
@Getter
@RequiredArgsConstructor
@ContainerEnum(namespace = "OrderItemStatusEnum",key = "status",value = "statusName")
public enum OrderItemStatusEnum {

    /**
     * 待支付
     */
    PENDING_PAYMENT(0,"待支付"),

    /**
     * 已支付
     */
    ALREADY_PAID(10,"已支付"),

    /**
     * 已进站
     */
    ALREADY_PULL_IN(20,"已进站"),

    /**
     * 已取消
     */
    CLOSED(30,"已取消"),

    /**
     * 已退票
     */
    REFUNDED(40,"已退票"),

    /**
     * 已改签
     */
    RESCHEDULED(50,"已改签");

    private final int status;

    private final String statusName;
}
