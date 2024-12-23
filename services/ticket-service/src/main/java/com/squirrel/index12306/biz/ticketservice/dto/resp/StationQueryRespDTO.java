package com.squirrel.index12306.biz.ticketservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 站点分页查询响应参数
 */
@Data
@Schema(description = "站点分页查询响应参数")
public class StationQueryRespDTO {

    /**
     * 名称
     */
    @Schema(description = "名称")
    private String name;

    /**
     * 地区编码
     */
    @Schema(description = "地区编码")
    private String code;

    /**
     * 拼音
     */
    @Schema(description = "拼音")
    private String spell;

    /**
     * 城市名称
     */
    @Schema(description = "城市名称")
    private String regionName;
}
