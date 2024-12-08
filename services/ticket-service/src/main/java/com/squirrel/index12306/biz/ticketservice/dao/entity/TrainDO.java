package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * 列车实体
 */
@Data
@TableName("t_train")
public class TrainDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 列车车次
     */
    @TableField("train_number")
    private String trainNumber;

    /**
     * 列车名称
     */
    @TableField("train_name")
    private String trainName;

    /**
     * 列车类型 0：高铁 1：动车 2：普通车
     */
    @TableField("train_type")
    private Integer trainType;

    /**
     * 列车标签: 复兴号 智能动车组 静音车厢 支持选铺
     */
    @TableField("train_tag")
    private String trainTag;

    /**
     * 列车品牌 0：GC-高铁/城际 1：D-动车 2：Z-直达 3：T-特快 4：K-快速 5：其他 6：复兴号 7：智能动车组
     */
    @TableField("train_brand")
    private String trainBrand;

    /**
     * 起始站
     */
    @TableField("start_station")
    private String startStation;

    /**
     * 终点站
     */
    @TableField("end_station")
    private String endStation;

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
     * 销售时间
     */
    @TableField("sale_time")
    private Date saleTime;

    /**
     * 销售状态 0：可售 1：不可售 2：未知
     */
    @TableField("sale_status")
    private Integer saleStatus;

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
