package com.squirrel.index12306.biz.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.squirrel.index12306.biz.ticketservice.dto.domain.BulletTrainDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 车票分页查询响应参数
 */
@Data
@Schema(description = "车票分页查询返回参数")
public class TicketPageQueryRespDTO {

    /**
     * 车次
     */
    @Schema(description = "车次")
    private String trainNumber;

    /**
     * 出发时间
     */
    @Schema(description = "出发时间")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 到达时间
     */
    @Schema(description = "到达时间")
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 历时
     */
    @Schema(description = "历时")
    private String duration;

    /**
     * 出发站点
     */
    @Schema(description = "出发站点")
    private String departure;

    /**
     * 到达站点
     */
    @Schema(description = "到达站点")
    private String arrival;

    /**
     * 始发站标识
     */
    @Schema(description = "始发站标识")
    private Boolean departureFlag;

    /**
     * 终点站标识
     */
    @Schema(description = "终点站标识")
    private Boolean arrivalFlag;

    /**
     * 高铁属性
     */
    @Schema(description = "高铁属性")
    private BulletTrainDTO bulletTrain;
}
