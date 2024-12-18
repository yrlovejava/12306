package com.squirrel.index12306.biz.payservice.dto.base;

import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.common.enums.PayTradeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 支付宝请求入参
 */
@Data
@Accessors(chain = true)
@Schema(description = "支付宝请求入参")
public final class AliPayRequest extends AbstractPayRequest {

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复
     */
    @Schema(description = "商户订单号")
    private String outOrderSn;

    /**
     * 订单总金额
     * 单位为元，精确到小数点后两位，取值范围：[0.01,100000000]
     */
    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    /**
     * 订单标题
     * 注意：不可使用特殊字符，如 /，=，& 等
     */
    @Schema(description = "订单标题")
    private String subject;

    /**
     * 交易凭证号
     */
    @Schema(description = "交易凭证号")
    private String tradeNo;

    @Override
    public AliPayRequest getAliPayRequest() {
        return this;
    }

    @Override
    public String buildMark() {
        String mark = PayChannelEnum.ALI_PAY.name();
        if (getTradeType() != null) {
            mark = PayChannelEnum.ALI_PAY.name() + "_" + PayTradeTypeEnum.findNameByCode(getTradeType());
        }
        return mark;
    }
}
