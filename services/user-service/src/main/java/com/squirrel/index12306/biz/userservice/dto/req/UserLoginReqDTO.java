package com.squirrel.index12306.biz.userservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户登录请求参数")
public class UserLoginReqDTO {

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
}
