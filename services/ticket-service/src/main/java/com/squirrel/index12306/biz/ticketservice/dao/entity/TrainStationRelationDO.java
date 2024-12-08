package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * 列车站点关系实体
 */
@Data
@TableName("train_station_relation")
public class TrainStationRelationDO extends BaseDO {

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
     * 始发标识
     */
    @TableField("departure_flag")
    private Integer departureFlag;

    /**
     * 终点标识
     */
    @TableField("arrival_flag")
    private Integer arrivalFlag;

    /**
     * 出发时间
     */
    @TableField("departure_time")
    private Date departureTime;

    /**
     * 到达时间
     */
    @TableField("arrival_time")
    private Date arrivalTime;
}
