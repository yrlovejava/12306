package com.squirrel.index12306.biz.userservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册请求参数
 */
@Data
@Schema(description = "用户注册请求参数")
public class UserRegisterReqDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 密码
     */
    @Schema(description = "密码")
    private String password;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 证件类型
     */
    @Schema(description = "证件类型")
    private Integer idType;

    /**
     * 证件号
     */
    @Schema(description = "证件号")
    private String idCard;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱")
    private String mail;

    /**
     * 旅客类型
     */
    @Schema(description = "旅客类型")
    private Integer userType;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态")
    private Integer verifyState;

    /**
     * 邮编
     */
    @Schema(description = "邮编")
    private String postCode;

    /**
     * 地址
     */
    @Schema(description = "地址")
    private String address;

    /**
     * 国家/地区
     */
    @Schema(description = "国家/地区")
    private String region;

    /**
     * 固定电话
     */
    @Schema(description = "固定电话")
    private String telephone;
}
