package com.squirrel.index12306.biz.orderservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单明细数据库实体
 */
@Data
@TableName("t_order_item")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    @TableField("order_sn")
    private String orderSn;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

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
     * 座位类型
     */
    @TableField("seat_type")
    private Integer seatType;

    /**
     * 座位号
     */
    @TableField("seat_number")
    private String seatNumber;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 证件类型
     */
    @TableField("id_type")
    private Integer idType;

    /**
     * 证件号
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 车票类型
     */
    @TableField("ticket_type")
    private Integer ticketType;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 订单状态
     */
    @TableField("status")
    private Integer status;

    /**
     * 订单金额
     */
    @TableField("amount")
    private Integer amount;
}
