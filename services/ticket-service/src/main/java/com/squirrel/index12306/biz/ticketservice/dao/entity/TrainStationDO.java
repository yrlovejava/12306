package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * 列车站点实体
 */
@Data
@TableName("t_train_station")
public class TrainStationDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 车次id
     */
    @TableField("train_id")
    private Long trainId;

    /**
     * 车站ID
     */
    @TableField("station_id")
    private Long stationId;

    /**
     * 站点顺序
     */
    @TableField("sequence")
    private Integer sequence;

    /**
     * 出发站点
     */
    @TableField("departure")
    private String departure;

    /**
     * 到达站点
     */
    @TableField("arrival")
    private String arrival;

    /**
     * 起始城市
     */
    @TableField("start_region")
    private String startRegion;

    /**
     * 终点城市
     */
    @TableField("end_region")
    private String endRegion;

    /**
     * 到站时间
     */
    @TableField("arrival_time")
    private Date arrivalTime;

    /**
     * 出站时间
     */
    @TableField("departure_time")
    private Date departureTime;

    /**
     * 停留时间，单位分
     */
    @TableField("stopover_time")
    private Integer stopoverTime;
}
