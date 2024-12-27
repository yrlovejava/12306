package com.squirrel.index12306.biz.orderservice.controller;

import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.orderservice.service.OrderService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 车票订单接口控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "车票订单接口控制层")
public class TicketOrderController {

    private final OrderService orderService;

    /**
     * 根据订单号查询车票订单
     * @param orderSn 订单号
     */
    @Operation(summary = "根据订单号查询车票订单")
    @GetMapping("/api/order-service/order/ticket/query")
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByOrderSn(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(orderService.queryTicketOrderByOrderSn(orderSn));
    }

    /**
     * 根据用户id查询车票订单
     * @param userId 用户id
     * @return Result<TicketOrderDetailRespDTO>
     */
    @Operation(summary = "根据用户id查询订单")
    @GetMapping("/api/order-service/order/ticket/query/userid")
    public Result<TicketOrderDetailRespDTO> queryTicketOrderByUserId(@RequestParam(value = "userid")String userId) {
        return Results.success(orderService.queryTicketOrderByUserId(userId));
    }

    /**
     * 车票订单创建
     * @param requestParam 订单创建参数
     * @return Result<String> 订单号
     */
    @Operation(summary = "车票订单创建")
    @PostMapping("/api/order-service/order/ticket/create")
    public Result<String> createTicketOrder(@RequestBody TicketOrderCreateReqDTO requestParam) {
        return Results.success(orderService.createTicketOrder(requestParam));
    }

    /**
     * 车票订单关闭
     * @param orderSn 订单号
     * @return Result<Void>
     */
    @Operation(summary = "车票订单关闭")
    @PostMapping("/api/order-service/order/ticket/close")
    public Result<Void> closeTickOrder(@RequestParam("orderSn") String orderSn) {
        orderService.closeTickOrder(orderSn);
        return Results.success();
    }

    /**
     * 车票订单取消
     * @param orderSn 订单号
     * @return  Result<Void>
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    public Result<Void> cancelTickOrder(@RequestParam("orderSn") String orderSn) {
        orderService.cancelTickOrder(orderSn);
        return Results.success();
    }
}
