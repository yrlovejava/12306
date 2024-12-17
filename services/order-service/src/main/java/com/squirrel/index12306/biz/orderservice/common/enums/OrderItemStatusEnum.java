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
     * 待进站
     */
    PENDING_PULL_IN(0),

    /**
     * 已进站
     */
    ALREADY_PULL_IN(20),

    /**
     * 已取消
     */
    CLOSED(30);

    private final int status;
}
