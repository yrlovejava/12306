package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 车票实体
 */
@Data
@TableName("t_ticket")
public class TicketDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    @TableField("username")
    private String username;

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
     * 乘客id
     */
    @TableField("passenger_id")
    private String passengerId;

    /**
     * 车票状态
     */
    @TableField("ticket_status")
    private Integer ticketStatus;
}
