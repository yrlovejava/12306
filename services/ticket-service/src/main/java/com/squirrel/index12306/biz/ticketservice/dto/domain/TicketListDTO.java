package com.squirrel.index12306.biz.ticketservice.dto.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 车次集合实体
 */
@Data
@Schema(description = "车次集合实体")
public class TicketListDTO {

    /**
     * 列车ID
     */
    @Schema(description = "列车ID")
    private String trainId;

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
     * 到达天数
     */
    @Schema(description = "到达天数")
    private Integer daysArrived;

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
     * 列车类型 0：高铁 1：动车 2：普通车
     */
    @Schema(description = "列车类型")
    private Integer trainType;

    /**
     * 可售时间
     */
    @Schema(description = "可售时间")
    @JsonFormat(pattern = "MM-dd HH:mm", timezone = "GMT+8")
    private Date saleTime;

    /**
     * 销售状态 0：可售 1：不可售 2：未知
     */
    @Schema(description = "销售状态")
    private Integer saleStatus;

    /**
     * 列车标签集合 0：复兴号 1：智能动车组 2：静音车厢 3：支持选铺
     */
    @Schema(description = "列车标签集合")
    private List<String> trainTag;

    /**
     * 高铁属性
     */
    @Schema(description = "高铁属性")
    private HighSpeedTrainDTO highSpeedTrain;

    /**
     * 动车属性
     */
    @Schema(description = "动车属性")
    private BulletTrainDTO bulletTrain;

    /**
     * 普通车属性
     */
    @Schema(description = "普通车属性")
    private RegularTrainDTO regularTrain;
}
