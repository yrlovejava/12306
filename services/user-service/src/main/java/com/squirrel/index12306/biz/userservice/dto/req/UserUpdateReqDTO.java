package com.squirrel.index12306.biz.userservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户修改请求参数
 */
@Data
@Schema(description = "用户修改请求参数")
public class UserUpdateReqDTO {

    /**
     * 用户ID
     */
    @Schema(description = "用户ID")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

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
