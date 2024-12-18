package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 站点路线实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "站点路线实体")
public class RouteDTO {

    /**
     * 出发站点
     */
    @Schema(description = "出发站点")
    private String startStation;

    /**
     * 目的站点
     */
    @Schema(description = "目的站点")
    private String endStation;
}
