package com.squirrel.index12306.biz.ticketservice.mq.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 延迟关闭订单事件
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DelayCloseOrderEvent {

    /**
     * 订单号
     */
    private String orderSn;
}
