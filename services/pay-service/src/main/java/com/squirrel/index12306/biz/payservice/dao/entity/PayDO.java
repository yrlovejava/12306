package com.squirrel.index12306.biz.payservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

import java.util.Date;

/**
 * 支付实体
 */
@Data
@TableName("t_pay")
public class PayDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 支付流水号
     */
    @TableField("pay_sn")
    private String paySn;

    /**
     * 订单号
     */
    @TableField("order_sn")
    private String orderSn;

    /**
     * 商户订单号
     */
    @TableField("out_order_sn")
    private String outOrderSn;

    /**
     * 支付渠道
     */
    @TableField("channel")
    private String channel;

    /**
     * 支付环境
     */
    @TableField("trade_type")
    private String tradeType;

    /**
     * 订单标题
     */
    @TableField("subject")
    private String subject;

    /**
     * 商户订单号
     * 由商家自定义，64个字符以内，仅支持字母、数字、下划线且需保证在商户端不重复
     */
    @TableField("order_request_id")
    private String orderRequestId;

    /**
     * 交易总金额
     */
    @TableField("total_amount")
    private Integer totalAmount;

    /**
     * 交易凭证号
     */
    @TableField("trade_no")
    private String tradeNo;

    /**
     * 付款时间
     */
    @TableField("gmt_payment")
    private Date gmtPayment;

    /**
     * 支付金额
     */
    @TableField("pay_amount")
    private Integer payAmount;

    /**
     * 支付状态
     */
    @TableField("status")
    private String status;
}
