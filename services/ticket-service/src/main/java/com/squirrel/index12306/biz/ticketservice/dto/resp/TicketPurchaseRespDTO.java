package com.squirrel.index12306.biz.ticketservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车票购买返回参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "车票购买返回参数")
public class TicketPurchaseRespDTO {

    /**
     * 订单号
     */
    @Schema(description = "订单号")
    private String orderSn;

    /**
     * 车票订单详情
     */
    @Schema(description = "车票订单详情")
    private List<TicketOrderDetailRespDTO> ticketOrderDetails;
}
