package com.squirrel.index12306.biz.userservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 乘车人移除请求参数
 */
@Data
@Schema(description = "乘车人移除请求参数")
public class PassengerRemoveReqDTO {

    /**
     * 乘车人id
     */
    @Schema(description = "乘车人id")
    private String id;
}
