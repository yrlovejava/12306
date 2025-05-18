package com.squirrel.index12306.biz.payservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 支付单详情信息返回参数
 */
@Data
@Schema(description = "支付单详情信息返回参数")
public class PayInfoRespDTO {

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderSn;

    /**
     * 支付总金额
     */
    @Schema(description = "支付总金额")
    private Integer totalAmount;

    /**
     * 支付状态
     */
    @Schema(description = "支付状态")
    private Integer status;

    /**
     * 支付时间
     */
    @Schema(description = "支付时间")
    private Date gmtPayment;
}
