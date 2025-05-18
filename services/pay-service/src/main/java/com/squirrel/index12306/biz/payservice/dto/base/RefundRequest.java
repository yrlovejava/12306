package com.squirrel.index12306.biz.payservice.dto.base;

/**
 * 退款接口
 */
public interface RefundRequest {

    /**
     * 获取支付宝退款请求参数
     * @return {@link AliRefundRequest}
     */
    AliRefundRequest getAliRefundRequest();

    /**
     * 获取订单号
     */
    String getOrderSn();

    /**
     * 构建查找支付策略实现类标识
     */
    String buildMark();
}
