package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 乘车人信息实体
 */
@Data
@Accessors(chain = true)
@Schema(description = "乘车人信息实体")
public class PassengerInfoDTO {

    /**
     * 乘车人 ID
     */
    @Schema(description = "乘车人ID")
    private String passengerId;

    /**
     * 乘车人姓名
     */
    @Schema(description = "乘车人姓名")
    private String realName;

    /**
     * 乘车人证件类型
     */
    @Schema(description = "乘车人证件类型")
    private Integer idType;

    /**
     * 乘车人证件号
     */
    @Schema(description = "乘车人证件号")
    private String idCard;

    /**
     * 乘车人手机号
     */
    @Schema(description = "乘车人手机号")
    private String phone;
}
