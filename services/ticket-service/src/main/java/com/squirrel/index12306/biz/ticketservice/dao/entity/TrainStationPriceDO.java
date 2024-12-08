package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 列车站点价格实体
 */
@Data
@TableName("t_train_station_price")
public class TrainStationPriceDO extends BaseDO {

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
     * 座位类型
     */
    @TableField("seat_type")
    private Integer seatType;

    /**
     * 车票价格
     */
    @TableField("price")
    private Integer price;
}
