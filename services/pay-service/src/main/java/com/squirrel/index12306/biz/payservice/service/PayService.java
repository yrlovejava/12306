package com.squirrel.index12306.biz.payservice.service;

import com.squirrel.index12306.biz.payservice.dto.req.PayCallbackReqDTO;
import com.squirrel.index12306.biz.payservice.dto.req.RefundReqDTO;
import com.squirrel.index12306.biz.payservice.dto.resp.PayInfoRespDTO;
import com.squirrel.index12306.biz.payservice.dto.resp.PayRespDTO;
import com.squirrel.index12306.biz.payservice.dto.base.PayRequest;
import com.squirrel.index12306.biz.payservice.dto.resp.RefundRespDTO;

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
     * 根据订单号查询支付单详情查询
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByOrderSn(String orderSn);

    /**
     * 跟据支付流水号查询支付单详情
     *
     * @param paySn 支付单流水号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfoByPaySn(String paySn);

    /**
     * 公共退款接口
     *
     * @param requestParam 退款请求参数
     * @return 退款响应参数
     */
    RefundRespDTO commonRefund(RefundReqDTO requestParam);
}
