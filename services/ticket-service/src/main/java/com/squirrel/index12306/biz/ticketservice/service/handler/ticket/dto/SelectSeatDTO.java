package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto;

import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 选择座位实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "选择座位实体")
public final class SelectSeatDTO {

    /**
     * 座位类型
     */
    @Schema(description = "座位类型")
    private Integer seatType;

    /**
     * 座位对应的乘车人集合
     */
    @Schema(description = "座位对应的乘车人集合")
    private List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails;

    /**
     * 购票原始入参
     */
    @Schema(description = "购票原始入参")
    private PurchaseTicketReqDTO requestParam;
}
