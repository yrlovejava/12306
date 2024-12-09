package com.squirrel.index12306.biz.ticketservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 地区&站点查询请求入参
 */
@Data
@Schema(description = "地区&站点查询请求入参")
public class RegionStationQueryReqDTO {

    /**
     * 查询方式
     */
    @Schema(description = "查询方式")
    private Integer queryType;

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;
}
