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

import java.util.Date;

/**
 * 订单数据库实体
 */
@Data
@TableName("t_order")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDO extends BaseDO {

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
     * 列车车次
     */
    @TableField("train_number")
    private String trainNumber;

    /**
     * 乘车日期
     */
    @TableField("riding_date")
    private Date ridingDate;

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
     * 订单来源
     */
    @TableField("source")
    private Integer source;

    /**
     * 订单状态
     */
    @TableField("status")
    private Integer status;

    /**
     * 下单时间
     */
    @TableField("order_time")
    private Date orderTime;

    /**
     * 支付方式
     */
    @TableField("pay_type")
    private Integer payType;

    /**
     * 支付时间
     */
    @TableField("pay_time")
    private Date payTime;
}
