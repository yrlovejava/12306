package com.squirrel.index12306.biz.ticketservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 车票订单详情返回参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "车票订单详情返回参数")
public class TicketOrderDetailRespDTO {

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
     * 证件号
     */
    @Schema(description = "证件号")
    private String idCard;

    /**
     * 车票类型 0：成人 1：儿童 2：学生 3：残疾军人
     */
    @Schema(description = "车票类型")
    private Integer ticketType;

    /**
     * 订单金额
     */
    @Schema(description = "订单金额")
    private Integer amount;
}
