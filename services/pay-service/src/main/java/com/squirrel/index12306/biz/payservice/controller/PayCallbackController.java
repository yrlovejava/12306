package com.squirrel.index12306.biz.payservice.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.convert.PayCallbackRequestConvert;
import com.squirrel.index12306.biz.payservice.dto.command.PayCallbackCommand;
import com.squirrel.index12306.biz.payservice.dto.base.PayCallbackRequest;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractStrategyChoose;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 支付结果回调
 */
@RestController
@RequiredArgsConstructor
public class PayCallbackController {

    private final AbstractStrategyChoose abstractStrategyChoose;

    /**
     * 支付宝回调
     * 调用支付宝支付后，支付宝会调用此接口发送支付结果
     */
    @PostMapping("/api/pay-service/callback/alipay")
    public void callbackAlipay(@RequestParam Map<String, Object> requestParam) {
        PayCallbackCommand payCallbackCommand = BeanUtil.mapToBean(requestParam, PayCallbackCommand.class, true, CopyOptions.create());
        // 支付渠道
        payCallbackCommand.setChannel(PayChannelEnum.ALI_PAY.getCode());
        // 订单号
        payCallbackCommand.setOrderRequestId(requestParam.get("out_trade_no").toString());
        // 买家付款时间
        payCallbackCommand.setGmtPayment(DateUtil.parse(requestParam.get("gmt_payment").toString()));
        // 获取回调参数
        PayCallbackRequest payCallbackRequest = PayCallbackRequestConvert.command2PayCallbackRequest(payCallbackCommand);
        /**
         * {@link AliPayCallbackHandler}
         */
        // 策略模式：通过策略模式封装支付回调渠道，支付回调时动态选择对应的支付回调组件
        abstractStrategyChoose.chooseAndExecute(payCallbackRequest.buildMark(), payCallbackRequest);
    }
}
