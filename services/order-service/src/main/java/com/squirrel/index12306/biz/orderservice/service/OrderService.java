package com.squirrel.index12306.biz.orderservice.service;

import com.squirrel.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;

/**
 * 订单接口层
 */
public interface OrderService {

    /**
     * 根据订单号查询车票订单
     *
     * @param orderSn 订单号
     * @return 订单详情
     */
    TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn);

    /**
     * 根据用户id查询车票订单
     *
     * @param userId 用户id
     * @return 订单详情
     */
    TicketOrderDetailRespDTO queryTicketOrderByUserId(String userId);

    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    String createTicketOrder(TicketOrderCreateReqDTO requestParam);

    /**
     * 关闭火车票订单
     *
     * @param orderSn 订单号
     */
    void closeTickOrder(String orderSn);

    /**
     * 取消火车票订单
     *
     * @param orderSn 订单号
     */
    void cancelTickOrder(String orderSn);

    /**
     * 订单状态反转
     *
     * @param requestParam 请求参数
     */
    void statusReversal(OrderStatusReversalDTO requestParam);

    /**
     * 支付结果回调订单
     *
     * @param requestParam 请求参数
     */
    void payCallbackOrder(PayResultCallbackOrderEvent requestParam);
}
