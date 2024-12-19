package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 购票乘车人详情实体
 */
@Data
@Accessors(chain = true)
@Schema(description = "乘车人信息实体")
public class PurchaseTicketPassengerDetailDTO {

    /**
     * 乘车人 ID
     */
    @Schema(description = "乘车人ID")
    private String passengerId;

    /**
     * 座位类型
     */
    @Schema(description = "座位类型")
    private Integer seatType;
}
