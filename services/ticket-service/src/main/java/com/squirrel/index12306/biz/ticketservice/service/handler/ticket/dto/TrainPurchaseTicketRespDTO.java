package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 列车购票出参
 */
@Data
@Schema(description = "列车购票出参")
public class TrainPurchaseTicketRespDTO {

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

    /**
     * 用户类型 0：成人 1：儿童 2：学生 3：残疾军人
     */
    @Schema(description = "用户类型")
    private Integer userType;

    /**
     * 席别类型
     */
    @Schema(description = "席别类型")
    private Integer seatType;

    /**
     * 车厢号
     */
    @Schema(description = "车厢号")
    private String carriageNumber;

    /**
     * 座位号
     */
    @Schema(description = "座位号")
    private String seatNumber;

    /**
     * 座位金额
     */
    @Schema(description = "座位金额")
    private Integer amount;
}
