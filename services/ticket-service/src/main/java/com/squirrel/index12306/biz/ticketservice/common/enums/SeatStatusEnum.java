package com.squirrel.index12306.biz.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 座位状态枚举
 */
@Getter
@RequiredArgsConstructor
public enum SeatStatusEnum {

    /**
     * 可售
     */
    AVAILABLE(0),

    /**
     * 锁定
     */
    LOCKED(1),

    /**
     * 已售
     */
    SOLD(2);

    private final Integer code;
}
