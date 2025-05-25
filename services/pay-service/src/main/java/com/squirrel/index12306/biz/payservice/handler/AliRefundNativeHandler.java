package com.squirrel.index12306.biz.payservice.handler;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeRefundModel;
import com.alipay.api.request.AlipayTradeRefundRequest;
import com.alipay.api.response.AlipayTradeRefundResponse;
import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.common.enums.PayTradeTypeEnum;
import com.squirrel.index12306.biz.payservice.common.enums.TradeStatusEnum;
import com.squirrel.index12306.biz.payservice.config.AliPayProperties;
import com.squirrel.index12306.biz.payservice.dto.base.AliRefundRequest;
import com.squirrel.index12306.biz.payservice.dto.base.RefundRequest;
import com.squirrel.index12306.biz.payservice.dto.base.RefundResponse;
import com.squirrel.index12306.biz.payservice.handler.base.AbstractRefundHandler;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 阿里支付组件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliRefundNativeHandler extends AbstractRefundHandler implements AbstractExecuteStrategy<RefundRequest, RefundResponse> {

    private final AliPayProperties aliPayProperties;

    private final static String SUCCESS_CODE = "10000";

    private final static String FUND_CHANGE = "Y";

    @SneakyThrows(value = AlipayApiException.class)
    @Override
    @Retryable(
            retryFor = {ServiceException.class},
            maxAttempts = 3,// 最大重试次数
            backoff = @Backoff(delay = 1000, multiplier = 1.5) // 第一次重试前延迟为1000ms，每次重试延迟时间是上一次的 1.5 倍
    )
    public RefundResponse refund(RefundRequest payRequest) {
        // 获取支付宝退款请求参数
        AliRefundRequest aliRefundRequest = payRequest.getAliRefundRequest();

        // 获取阿里支付client
        AlipayConfig alipayConfig = BeanUtil.convert(aliRefundRequest, AlipayConfig.class);
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        AlipayTradeRefundModel model = new AlipayTradeRefundModel();
        model.setOutTradeNo(aliRefundRequest.getOrderSn());
        model.setTradeNo(aliRefundRequest.getTradeNo());
        model.setRefundAmount(aliRefundRequest.getPayAmount().toString());
        AlipayTradeRefundRequest request = new AlipayTradeRefundRequest();
        request.setBizModel(model);
        try {
            AlipayTradeRefundResponse response = alipayClient.execute(request);
            String responseJson = JSONObject.toJSONString(response);
            log.info("发起支付宝退款，订单号：{}，交易凭证号：{}，退款金额：{} \n调用退款响应：\n\n{}\n",
                    aliRefundRequest.getOrderSn(),
                    aliRefundRequest.getTradeNo(),
                    aliRefundRequest.getPayAmount(),
                    responseJson);
            if (!StrUtil.equals(SUCCESS_CODE, response.getCode()) || !StrUtil.equals(FUND_CHANGE, response.getFundChange())) {
                throw new ServiceException("退款失败");
            }
            return RefundResponse.builder()
                    .status(TradeStatusEnum.TRADE_CLOSED.tradeCode())
                    .build();
        } catch (AlipayApiException e) {
            log.error("支付宝退款异常", e);
            throw new ServiceException("支付宝退款异常");
        }
    }

    @Override
    public String mark() {
        return StrBuilder.create()
                .append(PayChannelEnum.ALI_PAY.name())
                .append("_")
                .append(PayTradeTypeEnum.NATIVE.name())
                .append("_")
                .append(TradeStatusEnum.TRADE_SUCCESS.tradeCode())
                .toString();
    }

    @Override
    public RefundResponse executeResp(RefundRequest requestParam) {
        return refund(requestParam);
    }
}
