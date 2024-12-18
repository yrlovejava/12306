package com.squirrel.index12306.biz.payservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.payservice.common.enums.TradeStatusEnum;
import com.squirrel.index12306.biz.payservice.dao.entity.PayDO;
import com.squirrel.index12306.biz.payservice.dao.mapper.PayMapper;
import com.squirrel.index12306.biz.payservice.dto.PayCallbackReqDTO;
import com.squirrel.index12306.biz.payservice.dto.PayRespDTO;
import com.squirrel.index12306.biz.payservice.dto.base.PayRequest;
import com.squirrel.index12306.biz.payservice.dto.base.PayResponse;
import com.squirrel.index12306.biz.payservice.mq.event.PayResultCallbackOrderEvent;
import com.squirrel.index12306.biz.payservice.mq.producer.PayResultCallbackOrderSendProduce;
import com.squirrel.index12306.biz.payservice.service.PayService;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractStrategyChoose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * 支付接口层实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final PayMapper payMapper;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final PayResultCallbackOrderSendProduce payResultCallbackOrderSendProduce;

    /**
     * 创建支付单
     *
     * @param requestParam 创建支付单实体
     * @return 支付返回详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayRespDTO commonPay(PayRequest requestParam) {
        /**
         * {@link AliPayNativeHandler}
         */
        // 策略模式：通过策略模式封装支付渠道和支付场景，用户支付时动态选择对应的支付组件
        // 生成支付单
        PayResponse result = abstractStrategyChoose.chooseAndExecuteResp(requestParam.buildMark(), requestParam);
        // 转换为支付单实体
        PayDO insertPay = BeanUtil.convert(result, PayDO.class);
        // 设置支付单状态为等待付款
        insertPay.setStatus(TradeStatusEnum.WAIT_BUYER_PAY.tradeCode());
        // 计算精确的金额
        insertPay.setTotalAmount(requestParam.getTotalAmount().multiply(new BigDecimal("100")).setScale(0, RoundingMode.HALF_UP).intValue());
        // 插入数据库
        int insert = payMapper.insert(insertPay);
        if (insert <= 0) {
            log.error("支付单创建失败，支付聚合根: {}", JSON.toJSONString(requestParam));
            throw new ServiceException("支付单创建失败");
        }
        return BeanUtil.convert(result, PayRespDTO.class);
    }

    /**
     * 支付单回调
     *
     * @param requestParam 回调支付单实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void callbackPay(PayCallbackReqDTO requestParam) {
        // 查询数据库中的支付单
        PayDO payDO = payMapper.selectOne(Wrappers.lambdaQuery(PayDO.class)
                .eq(PayDO::getOrderRequestId, requestParam.getOrderRequestId()));
        // 判断支付单是否存在
        if(Objects.isNull(payDO)){
            log.error("支付单不存在,orderRequestId: {}",requestParam.getOrderRequestId());
            throw new ServiceException("支付单不存在");
        }
        // 设置交易凭证号
        payDO.setTradeNo(requestParam.getTradeNo());
        // 设置支付状态
        payDO.setStatus(requestParam.getStatus());
        // 设置总金额
        payDO.setPayAmount(requestParam.getPayAmount());
        // 设置支付时间
        payDO.setGmtPayment(requestParam.getGmtPayment());
        // 修改数据库中数据
        int result = payMapper.updateById(payDO);
        if(result <= 0){
            log.error("修改支付单支付结果失败，支付单信息：{}", JSON.toJSONString(payDO));
            throw new ServiceException("修改支付单支付结果失败");
        }
        // 交易成功，回调订单服务告知支付结果，修改订单流转状态
        if(Objects.equals(requestParam.getStatus(),TradeStatusEnum.TRADE_SUCCESS.tradeCode())){
            payResultCallbackOrderSendProduce.sendMessage(BeanUtil.convert(payDO, PayResultCallbackOrderEvent.class));
        }
    }
}
