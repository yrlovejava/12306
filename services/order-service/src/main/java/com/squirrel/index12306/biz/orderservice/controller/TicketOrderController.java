package com.squirrel.index12306.biz.orderservice.controller;

import cn.crane4j.annotation.AutoOperate;
import com.squirrel.index12306.biz.orderservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderSelfPageQueryReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailSelfRespDTO;
import com.squirrel.index12306.biz.orderservice.service.OrderService;
import com.squirrel.index12306.framework.starter.convention.page.PageResponse;
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
     * 根据用户id分页查询车票订单
     * @param requestParam 分页查询条件
     * @return Result<TicketOrderDetailRespDTO>
     */
    @Operation(summary = "根据用户id分页查询车票订单")
    @GetMapping("/api/order-service/order/ticket/page")
    public Result<PageResponse<TicketOrderDetailRespDTO>> queryTicketOrderByUserId(TicketOrderPageQueryReqDTO requestParam) {
        return Results.success(orderService.pageTicketOrder(requestParam));
    }

    /**
     * 分页查询本人车票订单
     */
    @AutoOperate(type = TicketOrderDetailRespDTO.class,on = "data.records")
    @Operation(summary = "分页查询本人车票订单")
    @GetMapping("/api/order-service/order/ticket/self/page")
    public Result<PageResponse<TicketOrderDetailSelfRespDTO>> pageSelfTicketOrder(TicketOrderSelfPageQueryReqDTO requestParam) {
        return Results.success(orderService.pageSelfTicketOrder(requestParam));
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
     * @param requestParam 订单号
     * @return Result<Boolean>
     */
    @Operation(summary = "车票订单关闭")
    @PostMapping("/api/order-service/order/ticket/close")
    public Result<Boolean> closeTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.closeTickOrder(requestParam));
    }

    /**
     * 车票订单取消
     * @param requestParam 订单号
     * @return  Result<Boolean>
     */
    @PostMapping("/api/order-service/order/ticket/cancel")
    public Result<Boolean> cancelTickOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        return Results.success(orderService.cancelTickOrder(requestParam));
    }
}
