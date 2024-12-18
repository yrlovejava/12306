package com.squirrel.index12306.biz.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 列车站点查询响应参数
 */
@Data
@Schema(description = "列车站点查询响应参数")
public class TrainStationQueryRespDTO {

    /**
     * 站序
     */
    @Schema(description = "站序")
    private String sequence;

    /**
     * 站名
     */
    @Schema(description = "站名")
    private String departure;

    /**
     * 到站时间
     */
    @Schema(description = "到站时间")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 出发时间
     */
    @Schema(description = "出发时间")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 停留时间
     */
    @Schema(description = "停留时间")
    private Integer stopoverTime;
}
