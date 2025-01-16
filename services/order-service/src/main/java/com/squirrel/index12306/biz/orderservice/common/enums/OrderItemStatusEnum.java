package com.squirrel.index12306.biz.orderservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 订单明细状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum OrderItemStatusEnum {

    /**
     * 待支付
     */
    PENDING_PAYMENT(0),

    /**
     * 已支付
     */
    ALREADY_PAID(10),

    /**
     * 已进站
     */
    ALREADY_PULL_IN(20),

    /**
     * 已取消
     */
    CLOSED(30),

    /**
     * 已退票
     */
    REFUNDED(40),

    /**
     * 已改签
     */
    RESCHEDULED(50);

    private final int status;
}
