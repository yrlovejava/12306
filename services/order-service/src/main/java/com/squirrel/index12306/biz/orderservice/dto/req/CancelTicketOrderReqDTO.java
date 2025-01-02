package com.squirrel.index12306.biz.orderservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 取消车票订单请求入参
 */
@Data
@Schema(description = "取消车票订单请求入参")
public class CancelTicketOrderReqDTO {

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderSn;
}
