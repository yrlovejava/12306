package com.squirrel.index12306.biz.payservice.convert;

import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.dto.command.PayCommand;
import com.squirrel.index12306.biz.payservice.dto.base.AliPayRequest;
import com.squirrel.index12306.biz.payservice.dto.base.PayRequest;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 支付请求入参转换器
 */
public final class PayRequestConvert {

    /**
     * {@link PayCommand} to {@link PayRequest}
     *
     * @param payCommand 支付请求参数
     * @return {@link PayRequest}
     */
    public static PayRequest command2PayRequest(PayCommand payCommand) {
        PayRequest payRequest = null;
        if (Objects.equals(payCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())) {
            payRequest = BeanUtil.convert(payCommand, AliPayRequest.class);
        }
        return payRequest;
    }
}
