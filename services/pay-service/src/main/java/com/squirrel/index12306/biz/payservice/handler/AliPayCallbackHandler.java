package com.squirrel.index12306.biz.payservice.handler;

import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.common.enums.TradeStatusEnum;
import com.squirrel.index12306.biz.payservice.dto.req.PayCallbackReqDTO;
import com.squirrel.index12306.biz.payservice.dto.base.AliPayCallbackRequest;
import com.squirrel.index12306.biz.payservice.dto.base.PayCallbackRequest;
import com.squirrel.index12306.biz.payservice.handler.base.AbstractPayCallbackHandler;
import com.squirrel.index12306.biz.payservice.service.PayService;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 阿里支付回调组件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public final class AliPayCallbackHandler extends AbstractPayCallbackHandler implements AbstractExecuteStrategy<PayCallbackRequest,Void> {

    private final PayService payService;

    /**
     * 支付回调抽象接口
     *
     * @param payCallbackRequest 支付回调请求参数
     */
    @Override
    public void callback(PayCallbackRequest payCallbackRequest) {
        AliPayCallbackRequest aliPayCallBackRequest = payCallbackRequest.getAliPayCallBackRequest();
        PayCallbackReqDTO payCallbackRequestParam = PayCallbackReqDTO.builder()
                .status(TradeStatusEnum.queryActualTradeStatusCode(aliPayCallBackRequest.getTradeStatus()))
                .payAmount(aliPayCallBackRequest.getBuyerPayAmount())
                .tradeNo(aliPayCallBackRequest.getTradeNo())
                .gmtPayment(aliPayCallBackRequest.getGmtPayment())
                .orderSn(aliPayCallBackRequest.getOrderRequestId())
                .build();
        payService.callbackPay(payCallbackRequestParam);
    }

    /**
     * 执行策略标识
     * @return 策略标识
     */
    @Override
    public String mark() {
        return PayChannelEnum.ALI_PAY.name();
    }

    /**
     * 执行策略
     * @param requestParam 执行策略入参
     */
    @Override
    public void execute(PayCallbackRequest requestParam) {
        this.callback(requestParam);
    }
}
