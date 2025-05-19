package com.squirrel.index12306.biz.ticketservice.remote;

import com.squirrel.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 车票订单远程服务调用
 */
@FeignClient(value = "index12306-order-service",url = "${aggregation.remote-url}")
@Tag(name = "车票订单远程服务调用")
public interface TicketOrderRemoteService {

    /**
     * 跟据订单号查询车票订单
     *
     * @param orderSn 列车订单号
     * @return 列车订单记录
     */
    @GetMapping("/api/order-service/order/ticket/query")
    Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn);

    /**
     * 创建车票订单
     *
     * @param requestParam 创建车票订单请求参数
     * @return 订单号
     */
    @Operation(summary = "创建车票订单")
    @PostMapping("/api/order-service/order/ticket/create")
    Result<String> createTicketOrder(@RequestBody TicketOrderCreateRemoteReqDTO requestParam);

    /**
     * 车票订单关闭
     *
     * @param requestParam 车票订单关闭入参
     * @return 关闭订单返回结果
     */
    @Operation(summary = "车票订单关闭")
    @PostMapping("/api/order-service/order/ticket/close")
    Result<Boolean> closeTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam);

    /**
     * 车票订单取消
     *
     * @param requestParam 车票订单取消入参
     * @return 订单取消返回结果
     */
    @Operation(summary = "车票订单取消")
    @PostMapping("/api/order-service/order/ticket/cancel")
    Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam);
}
