package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 车厢实体
 */
@Data
@TableName("t_carriage")
public class CarriageDO extends BaseDO {

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
     * 车厢类型
     */
    @TableField("carriage_type")
    private Integer carriageType;

    /**
     * 座位数
     */
    @TableField("seat_count")
    private Integer seatCount;
}
