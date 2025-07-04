package com.squirrel.index12306.biz.ticketservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TicketDO;
import com.squirrel.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车票接口
 */
public interface TicketService extends IService<TicketDO> {

    /**
     * 根据条件分页查询车票
     *
     * @param requestParam 分页查询车票请求参数
     * @return 查询车票返回结果
     */
    TicketPageQueryRespDTO pageListTicketQuery(TicketPageQueryReqDTO requestParam);

    /**
     * 购买车票
     *
     * @param requestParam 车票购买请求参数
     * @return 订单号
     */
    TicketPurchaseRespDTO purchaseTicketsV1(@RequestBody PurchaseTicketReqDTO requestParam);

    /**
     * 购买车票
     *
     * @param requestParam 车票购买请求参数
     * @return 订单号
     */
    TicketPurchaseRespDTO purchaseTicketsV2(@RequestBody PurchaseTicketReqDTO requestParam);

    /**
     * 执行购买车票
     * 被对应购票版本号接口调用 {@link TicketService#purchaseTicketsV1(PurchaseTicketReqDTO)} and {@link TicketService#purchaseTicketsV2(PurchaseTicketReqDTO)}
     *
     * @param requestParam 车票购买请求参数
     * @return 订单号
     */
    TicketPurchaseRespDTO executePurchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam);

    /**
     * 支付单详情查询
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    PayInfoRespDTO getPayInfo(String orderSn);

    /**
     * 取消车票订单
     *
     * @param requestParam 取消车票订单入参
     */
    void cancelTicketOrder(CancelTicketOrderReqDTO requestParam);
}
