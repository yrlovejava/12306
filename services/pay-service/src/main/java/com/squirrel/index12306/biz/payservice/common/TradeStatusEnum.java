package com.squirrel.index12306.biz.payservice.common;

import cn.hutool.core.collection.ListUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 交易状态枚举
 */
public enum TradeStatusEnum {

    /**
     * 交易创建，等待买家付款
     */
    WAIT_BUYER_PAY {
        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("WAIT_BUYER_PAY");
        }
    },

    /**
     * 未付款交易超时关闭，或支付完成后全额退款
     */
    TRADE_CLOSED {

        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("TRADE_CLOSED");
        }
    },

    /**
     * 交易支付成功
     */
    TRADE_SUCCESS {

        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("TRADE_SUCCESS");
        }
    },

    /**
     * 交易结束，不可退款
     */
    TRADE_FINISHED {

        @Override
        protected List<String> tradeStatus() {
            return ListUtil.of("TRADE_FINISHED");
        }
    };

    /**
     * 获取交易状态集合
     */
    protected abstract List<String> tradeStatus();

    /**
     * 查询真实的交易状态
     * @param tradeStatus 三方交易状态
     * @return 真实的交易状态，入库使用
     */
    public static String queryActualTradeStatus(String tradeStatus) {
        Optional<TradeStatusEnum> tradeStatusEnum = Arrays.stream(TradeStatusEnum.values())
                .filter(each -> each.tradeStatus().contains(tradeStatus))
                .findFirst();
        return tradeStatusEnum.orElseThrow(() -> new ServiceException("未找到支付状态")).toString();
    }
}
