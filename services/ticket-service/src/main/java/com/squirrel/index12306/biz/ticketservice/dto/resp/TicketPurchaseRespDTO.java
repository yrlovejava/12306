package com.squirrel.index12306.biz.ticketservice.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车票购买返回参数
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TicketPurchaseRespDTO {

    /**
     * 订单号
     */
    private String orderSn;
}
