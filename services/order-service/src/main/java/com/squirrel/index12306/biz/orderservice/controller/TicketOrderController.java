package com.squirrel.index12306.biz.orderservice.controller;

import com.squirrel.index12306.biz.orderservice.dto.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.service.OrderService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车票订单接口控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "车票订单接口控制层")
public class TicketOrderController {

    private final OrderService orderService;

    /**
     * 车票订单创建
     */
    @Operation(summary = "车票订单创建")
    @PostMapping("/api/order-service/order/ticket/create")
    public Result<String> createTicketOrder(@RequestBody TicketOrderCreateReqDTO requestParam) {
        return Results.success(orderService.createTicketOrder(requestParam));
    }
}
