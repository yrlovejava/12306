package com.squirrel.index12306.biz.payservice.service;

import com.squirrel.index12306.biz.payservice.dto.PayCallbackReqDTO;
import com.squirrel.index12306.biz.payservice.dto.PayInfoRespDTO;
import com.squirrel.index12306.biz.payservice.dto.PayRespDTO;
import com.squirrel.index12306.biz.payservice.dto.base.PayRequest;

/**
 * 支付层接口
 */
public interface PayService {

    /**
     * 创建支付单
     *
     * @param requestParam 创建支付单实体
     * @return 支付返回详情
     */
    PayRespDTO commonPay(PayRequest requestParam);

    /**
     * 支付单回调
     *
     * @param requestParam 回调支付单实体
     */
    void callbackPay(PayCallbackReqDTO requestParam);

    /**
     * 支付单详情查询
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfo(String orderSn);
}
