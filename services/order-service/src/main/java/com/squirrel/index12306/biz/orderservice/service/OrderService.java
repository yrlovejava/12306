package com.squirrel.index12306.biz.orderservice.service;

import com.squirrel.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.squirrel.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.squirrel.index12306.framework.starter.convention.page.PageResponse;

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
     * 根据用户id分页查询车票订单
     *
     * @param requestParam 分页查询条件
     * @return 订单详情
     */
    PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam);

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
     * @param requestParam 关闭火车票订单入参
     */
    boolean closeTickOrder(CancelTicketOrderReqDTO requestParam);

    /**
     * 取消火车票订单
     *
     * @param requestParam 取消火车票订单入参
     */
    boolean cancelTickOrder(CancelTicketOrderReqDTO requestParam);

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

    /**
     * 查询本人车票订单
     *
     * @param requestParam 请求参数
     * @return 本人车票订单集合
     */
    PageResponse<TicketOrderDetailSelfRespDTO> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam);
}
