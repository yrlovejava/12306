package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.filter.purchase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 购票流程过滤器之验证乘客是否反复购买
 */
@Component
@RequiredArgsConstructor
public class TrainPurchaseTicketRepeatChainHandler implements TrainPurchaseTicketChainFilter {

    /**
     * 判断似乎否重复购买
     * @param requestParam 责任链执行入参
     */
    @Override
    public void handler(Object requestParam) {
        // TODO 重复购买验证后续实现
    }

    /**
     * 设置优先级
     */
    @Override
    public int getOrder() {
        return 30;
    }
}
