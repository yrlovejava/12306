package com.squirrel.index12306.biz.userservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录返回参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录返回参数")
public class UserLoginRespDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户登录")
    private String username;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * Token
     */
    @Schema(description = "Token")
    private String accessToken;
}
