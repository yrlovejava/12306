package com.squirrel.index12306.biz.ticketservice.controller;

import com.squirrel.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.RegionStationService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 地区以及车站查询控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "地区以及车站查询控制层")
public class RegionStationController {

    private final RegionStationService regionStationService;

    /**
     * 查询车站&城市站点集合信息
     */
    @Operation(summary = "查询车站&城市站点集合信息")
    @GetMapping("/api/ticket-service/region-station/query")
    public Result<List<RegionStationQueryRespDTO>> listRegionStation(RegionStationQueryReqDTO requestParam) {
        return Results.success(regionStationService.listRegionStation(requestParam));
    }

    /**
     * 查询车站站点集合信息
     */
    @Operation(summary = "查询车站站点集合信息")
    @GetMapping("/api/ticket-service/station/all")
    public Result<List<StationQueryRespDTO>> listAllStation() {
        return Results.success(regionStationService.listAllStation());
    }
}
