package com.squirrel.index12306.biz.payservice.dto.command;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.squirrel.index12306.biz.payservice.dto.base.AbstractPayRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付回调请求命令
 */
@Data
@Schema(description = "支付回调请求命令")
public final class PayCallbackCommand extends AbstractPayRequest {

    /**
     * 支付渠道
     */
    @Schema(description = "支付渠道")
    private Integer channel;

    /**
     * 支付状态
     */
    @Schema(description = "支付状态")
    @JsonAlias("trade_status")
    private String tradeStatus;

    /**
     * 支付凭证号
     */
    @Schema(description = "支付凭证号")
    @JsonAlias("trade_no")
    private String tradeNo;

    /**
     * 买家付款时间
     */
    @Schema(description = "买家付款时间")
    @JsonAlias("gmt_payment")
    private Date gmtPayment;

    /**
     * 买家付款金额
     */
    @Schema(description = "买家付款金额")
    @JsonAlias("buyer_pay_amount")
    private BigDecimal buyerPayAmount;

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复
     */
    @Schema(description = "商户订单号")
    @JsonAlias("out_trade_no")
    private String outTradeNo;

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
}
