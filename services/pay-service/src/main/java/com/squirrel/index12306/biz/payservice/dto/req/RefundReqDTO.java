package com.squirrel.index12306.biz.payservice.dto.req;

import lombok.Data;

/**
 * 退款请求参数
 */
@Data
public class RefundReqDTO {

    /**
     * 订单号
     */
    private String orderSn;
}
