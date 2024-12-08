package com.squirrel.index12306.biz.ticketservice.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TicketService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public Result<IPage<TicketPageQueryRespDTO>> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        return Results.success(ticketService.pageListTicketQuery(requestParam));
    }
}
