package com.squirrel.index12306.biz.userservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注册返回参数
 */
@Data
@Schema(description = "用户注册返回参数")
public class UserRegisterRespDTO {

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
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
}
