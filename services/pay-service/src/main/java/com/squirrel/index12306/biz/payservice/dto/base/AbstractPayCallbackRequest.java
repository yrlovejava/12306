package com.squirrel.index12306.biz.payservice.dto.base;

import lombok.Getter;
import lombok.Setter;

/**
 * 抽象支付回调入参实体
 */
@Setter
@Getter
public abstract class AbstractPayCallbackRequest implements PayCallbackRequest {

    /**
     * 商户订单号
     */
    private String orderRequestId;

    @Override
    public AliPayCallbackRequest getAliPayCallBackRequest() {
        return null;
    }

    @Override
    public String buildMark() {
        return null;
    }
}
