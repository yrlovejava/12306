package com.squirrel.index12306.biz.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 用户信息实体
 */
@Data
@TableName("t_user")
public class UserDO extends BaseDO {

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
     * 密码
     */
    @TableField("password")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 国家/地区
     */
    @TableField("region")
    private String region;

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
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 固定电话
     */
    @TableField("telephone")
    private String telephone;

    /**
     * 邮箱
     */
    @TableField("mail")
    private String mail;

    /**
     * 旅客类型
     */
    @TableField("user_type")
    private Integer userType;

    /**
     * 审核状态
     */
    @TableField("verify_status")
    private Integer verifyStatus;

    /**
     * 邮编
     */
    @TableField("post_code")
    private String postCode;

    /**
     * 地址
     */
    @TableField("address")
    private String address;

    /**
     * 注销时间戳
     */
    @TableField("deletion_time")
    private Long deletionTime;
}
