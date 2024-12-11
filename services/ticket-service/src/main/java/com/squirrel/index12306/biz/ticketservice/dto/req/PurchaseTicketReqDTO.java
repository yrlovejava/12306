package com.squirrel.index12306.biz.ticketservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 购买请求入参
 */
@Data
@Schema(description = "购买请求入参")
public class PurchaseTicketReqDTO {

    /**
     * 车次 ID
     */
    @Schema(description = "车次ID")
    private String trainId;

    /**
     * 乘车人
     */
    @Schema(description = "乘车人")
    private List<String> passengerIds;

    /**
     * 座位类型
     */
    @Schema(description = "座位类型")
    private Integer seatType;

    /**
     * 选择座位
     */
    @Schema(description = "选择座位")
    private List<String> chooseSeats;

    /**
     * 出发站点
     */
    @Schema(description = "出发站点")
    private String departure;

    /**
     * 到达站点
     */
    @Schema(description = "到达站点")
    private String arrival;
}
