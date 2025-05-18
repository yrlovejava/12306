package com.squirrel.index12306.biz.payservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 支付单创建请求参数
 */
@Data
@Schema(description = "支付单创建请求参数")
public class PayCreateReqDTO {

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderSn;

    /**
     * 商户订单号
     */
    @Schema(description = "商户订单号")
    private String outOrderSn;

    /**
     * 支付渠道
     */
    @Schema(description = "支付渠道")
    private String channel;

    /**
     * 支付环境
     */
    @Schema(description = "支付环境")
    private String tradeType;

    /**
     * 订单标题
     */
    @Schema(description = "订单标题")
    private String subject;

    /**
     * 交易凭证号
     */
    @Schema(description = "交易凭证号")
    private String tradeNo;

    /**
     * 交易总金额
     */
    @Schema(description = "交易总金额")
    private Integer totalAmount;

    /**
     * 付款时间
     */
    @Schema(description = "付款时间")
    private Date gmtPayment;

    /**
     * 支付金额
     */
    @Schema(description = "交付金额")
    private Integer payAmount;

    /**
     * 支付状态
     */
    @Schema(description = "支付状态")
    private String status;

    /**
     * 商户订单号
     */
    @Schema(description = "商户订单号")
    private String orderRequestId;
}
