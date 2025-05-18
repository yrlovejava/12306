package com.squirrel.index12306.biz.payservice.convert;

import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.dto.base.AliRefundRequest;
import com.squirrel.index12306.biz.payservice.dto.base.RefundRequest;
import com.squirrel.index12306.biz.payservice.dto.command.RefundCommand;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;

import java.util.Objects;

/**
 * 退款请求入参转换器
 */
public final class RefundRequestConvert {

    /**
     * {@link RefundCommand} to {@link RefundRequest}
     *
     * @param refundCommand 退款请求参数
     * @return {@link RefundRequest}
     */
    public static RefundRequest command2RefundRequest(RefundCommand refundCommand) {
        RefundRequest refundRequest = null;
        if(Objects.equals(refundCommand.getChannel(), PayChannelEnum.ALI_PAY.getCode())){
            refundRequest = BeanUtil.convert(refundCommand, AliRefundRequest.class);
        }
        return refundRequest;
    }
}
