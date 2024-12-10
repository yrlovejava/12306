package com.squirrel.index12306.biz.userservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 乘车人添加&修改请求参数
 */
@Data
@Schema(description = "乘车人添加&修改请求参数")
public class PassengerReqDTO {

    /**
     * 乘车人id
     */
    @Schema(description = "乘车人id")
    private String id;

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
     * 证件号码
     */
    @Schema(description = "证件号码")
    private String idCard;

    /**
     * 优惠类型
     */
    @Schema(description = "优惠类型")
    private Integer discountType;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;
}
