package com.squirrel.index12306.biz.payservice.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 抽象支付回调入参实体
 */
@Setter
@Getter
@Schema(description = "抽象支付回调入参实体")
public abstract class AbstractPayCallbackRequest implements PayCallbackRequest {

    /**
     * 商户订单号
     */
    @Schema(description = "商户订单号")
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
