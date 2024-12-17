package com.squirrel.index12306.biz.orderservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 支付渠道枚举
 */
@Getter
@RequiredArgsConstructor
public enum PayChannelEnum {

    ALI_PAY(0, "ALI_PAY", "支付宝");

    private final Integer code;

    private final String name;

    private final String value;
}
