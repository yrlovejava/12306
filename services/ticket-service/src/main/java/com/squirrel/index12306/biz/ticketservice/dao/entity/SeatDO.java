package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 座位实体
 */
@Data
@TableName("t_seat")
public class SeatDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 列车id
     */
    @TableField("train_id")
    private Long trainId;

    /**
     * 车厢号
     */
    @TableField("carriage_number")
    private String carriageNumber;

    /**
     * 座位号
     */
    @TableField("seat_number")
    private String seatNumber;

    /**
     * 座位类型
     */
    @TableField("seat_type")
    private Integer seatType;

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
     * 座位状态
     */
    @TableField("seat_status")
    private Integer seatStatus;

    /**
     * 车票价格
     */
    @TableField("price")
    private Integer price;
}
