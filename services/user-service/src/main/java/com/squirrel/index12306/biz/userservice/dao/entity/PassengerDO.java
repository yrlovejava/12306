package com.squirrel.index12306.biz.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_passenger")
public class PassengerDO extends BaseDO {

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
     * 证件号码
     */
    @TableField("id_card")
    private String idCard;

    /**
     * 优惠类型
     */
    @TableField("discount_type")
    private Integer discountType;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 添加日期
     */
    @TableField("create_date")
    private Date createDate;

    /**
     * 审核状态
     */
    @TableField("verify_status")
    private Integer verifyStatus;
}
