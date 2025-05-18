package com.squirrel.index12306.biz.payservice.dto.command;

import com.squirrel.index12306.biz.payservice.dto.base.AbstractPayRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 支付请求命令
 */
@Data
@Schema(description = "支付请求命令")
public final class PayCommand extends AbstractPayRequest {

    /**
     * 子订单号
     */
    @Schema(description = "子订单号")
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
}
