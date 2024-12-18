package com.squirrel.index12306.biz.ticketservice.dto.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 车次集合实体
 */
@Data
public class TicketListDTO {

    /**
     * 车次
     */
    private String trainNumber;

    /**
     * 出发时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date departureTime;

    /**
     * 到达时间
     */
    @JsonFormat(pattern = "HH:mm", timezone = "GMT+8")
    private Date arrivalTime;

    /**
     * 历时
     */
    private String duration;

    /**
     * 出发站点
     */
    private String departure;

    /**
     * 到达站点
     */
    private String arrival;

    /**
     * 始发站标识
     */
    private Boolean departureFlag;

    /**
     * 终点站标识
     */
    private Boolean arrivalFlag;

    /**
     * 列车类型 0：高铁 1：动车 2：普通车
     */
    private Integer trainType;

    /**
     * 可售时间
     */
    @JsonFormat(pattern = "MM-dd HH:mm", timezone = "GMT+8")
    private Date saleTime;

    /**
     * 销售状态 0：可售 1：不可售 2：未知
     */
    private Integer saleStatus;

    /**
     * 高铁属性
     */
    private HighSpeedTrainDTO highSpeedTrain;

    /**
     * 动车属性
     */
    private BulletTrainDTO bulletTrain;

    /**
     * 普通车属性
     */
    private RegularTrainDTO regularTrain;
}
