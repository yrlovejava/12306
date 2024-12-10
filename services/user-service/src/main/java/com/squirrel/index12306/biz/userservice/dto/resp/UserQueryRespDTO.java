package com.squirrel.index12306.biz.userservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询返回参数
 */
@Data
@Schema(description = "用户查询返回参数")
public class UserQueryRespDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

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
    private Integer verifyStatus;

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
}
