package com.squirrel.index12306.biz.userservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户注销请求参数
 */
@Data
@Schema(description = "用户注销请求参数")
public class UserDeletionReqDTO {

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;
}
