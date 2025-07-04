package com.squirrel.index12306.biz.payservice.handler;

import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.StrUtil;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.AlipayConfig;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.squirrel.index12306.biz.payservice.common.enums.PayChannelEnum;
import com.squirrel.index12306.biz.payservice.common.enums.PayTradeTypeEnum;
import com.squirrel.index12306.biz.payservice.config.AliPayProperties;
import com.squirrel.index12306.biz.payservice.dto.base.AliPayRequest;
import com.squirrel.index12306.biz.payservice.dto.base.PayRequest;
import com.squirrel.index12306.biz.payservice.dto.base.PayResponse;
import com.squirrel.index12306.biz.payservice.handler.base.AbstractPayHandler;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * 阿里支付组件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AliPayNativeHandler extends AbstractPayHandler implements AbstractExecuteStrategy<PayRequest, PayResponse> {

    private final AliPayProperties aliPayProperties;

    /**
     * 阿里支付
     *
     * @param payRequest 支付请求参数
     * @return 支付响应参数
     */
    @SneakyThrows(value = AlipayApiException.class)
    @Override
    @Retryable(
            retryFor = {ServiceException.class},
            maxAttempts = 3,// 最大重试次数
            backoff = @Backoff(delay = 1000, multiplier = 1.5) // 第一次重试前延迟为1000ms，每次重试延迟时间是上一次的 1.5 倍
    )
    public PayResponse pay(PayRequest payRequest) {
        AliPayRequest aliPayRequest = payRequest.getAliPayRequest();
        AlipayConfig alipayConfig = BeanUtil.convert(aliPayProperties, AlipayConfig.class);
        // 创建支付宝客户端
        AlipayClient alipayClient = new DefaultAlipayClient(alipayConfig);
        // 构建支付请求模型
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        // 商户订单号
        model.setOutTradeNo(aliPayRequest.getOrderSn());
        // 总支付金额
        model.setTotalAmount(aliPayRequest.getTotalAmount().toString());
        // 订单标题
        model.setSubject(aliPayRequest.getSubject());
        // 产品码，FAST_INSTANT_TRADE_PAY 即时到账的支付方式
        model.setProductCode("FAST_INSTANT_TRADE_PAY");

        // 创建支付宝支付请求对象
        AlipayTradePayRequest request = new AlipayTradePayRequest();
        // 回调url
        request.setNotifyUrl(aliPayProperties.getNotifyUrl());
        // 支付请求的业务模型
        request.setBizModel(model);
        try {
            // 执行页面支付请求并返回响应
            AlipayTradePayResponse response = alipayClient.pageExecute(request);
            log.info("发起支付宝支付，订单号：{}，子订单号：{}，订单请求号：{}，订单金额：{} \n调用支付返回：\n\n{}\n",
                    aliPayRequest.getOrderSn(),
                    aliPayRequest.getOutOrderSn(),
                    aliPayRequest.getOrderRequestId(),
                    aliPayRequest.getTotalAmount(),
                    response.getBody());
            if (!response.isSuccess()) {
                throw new ServiceException("调用支付宝发起支付异常");
            }
            return new PayResponse(StrUtil.replace(StrUtil.replace(response.getBody(), "\"", "'"), "\n", ""));
        } catch (AlipayApiException ex) {
            throw new ServiceException("调用支付宝支付异常");
        }
    }

    @Override
    public String mark() {
        return StrBuilder.create()
                .append(PayChannelEnum.ALI_PAY.name())
                .append("_")
                .append(PayTradeTypeEnum.NATIVE.name())
                .toString();
    }

    /**
     * 执行策略
     *
     * @param requestParam 执行策略入参
     * @return 支付结果
     */
    @Override
    public PayResponse executeResp(PayRequest requestParam) {
        return this.pay(requestParam);
    }
}
