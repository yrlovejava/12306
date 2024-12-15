package com.squirrel.index12306.biz.payservice.common;

/**
 * 交易环境枚举
 */
public enum PayTradeTypeEnum {

    /**
     * 扫码支付
     */
    NATIVE,

    /**
     * JS API 支付
     */
    JSAPI,

    /**
     * 移动网页支付
     */
    MWEB,

    /**
     * 去中心化应用支付
     */
    DAPP,
}
