package com.squirrel.index12306.biz.ticketservice.controller;

import com.squirrel.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TicketService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.idempotent.annotation.Idempotent;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import com.squirrel.index12306.framework.starter.log.annotation.ILog;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 车票控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "车票控制层")
public class TicketController {
    private final TicketService ticketService;

    /**
     * 根据条件查询车票
     * @param requestParam 分页查询条件
     * @return Result<IPage<TicketPageQueryRespDTO>>
     */
    @GetMapping("/api/ticket-service/ticket/query")
    @Operation(summary = "根据条件查询车票")
    public Result<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQuery(requestParam));
    }

    /**
     * 购买车票
     * @param requestParam 购买车票请求参数
     */
    @ILog
    @Operation(description = "购买车票")
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(com.squirrel.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @PostMapping("/api/ticket-service/ticket/purchase")
    public Result<TicketPurchaseRespDTO> purchaseTickets(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV1(requestParam));
    }

    /**
     * 购买车票V2接口
     * @param requestParam 购买车票请求参数
     * @return Result<TicketPurchaseRespDTO>
     */
    @ILog
    @Operation(description = "购买车票V2接口")
    @Idempotent(
            uniqueKeyPrefix = "index12306-ticket:lock_purchase-tickets:",
            key = "T(com.squirrel.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            message = "正在执行下单流程，请稍后...",
            scene = IdempotentSceneEnum.RESTAPI,
            type = IdempotentTypeEnum.SPEL
    )
    @PostMapping("/api/ticket-service/ticket/purchase/v2")
    public Result<TicketPurchaseRespDTO> purchaseTicketsV2(@RequestBody PurchaseTicketReqDTO requestParam) {
        return Results.success(ticketService.purchaseTicketsV2(requestParam));
    }

    /**
     * 取消车票订单
     */
    @ILog
    @PostMapping("/api/ticket-service/ticket/cancel")
    public Result<Void> cancelTicketOrder(@RequestBody CancelTicketOrderReqDTO requestParam) {
        ticketService.cancelTicketOrder(requestParam);
        return Results.success();
    }

    /**
     * 支付单详情查询
     */
    @Operation(description = "支付单详情查询")
    @GetMapping("/api/ticket-service/ticket/pay/query")
    public Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(ticketService.getPayInfo(orderSn));
    }
}
