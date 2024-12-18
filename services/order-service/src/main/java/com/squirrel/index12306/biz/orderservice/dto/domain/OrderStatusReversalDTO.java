package com.squirrel.index12306.biz.orderservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单状态反转实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "订单状态反转实体")
public final class OrderStatusReversalDTO {

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderSn;

    /**
     * 订单反转后状态
     */
    @Schema(description = "订单反转后状态")
    private Integer orderStatus;
}
