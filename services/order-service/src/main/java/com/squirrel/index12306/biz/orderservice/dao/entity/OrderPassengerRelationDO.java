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
 * 乘车人订单关系实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_order_passenger_relation")
public class OrderPassengerRelationDO extends BaseDO {

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
     * 证件类型
     */
    @TableField("id_type")
    private Integer idType;

    /**
     * 证件号
     */
    @TableField("id_card")
    private String idCard;
}
