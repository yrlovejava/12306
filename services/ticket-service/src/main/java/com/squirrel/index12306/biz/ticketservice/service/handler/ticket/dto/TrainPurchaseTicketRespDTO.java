package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto;

import com.squirrel.index12306.biz.ticketservice.dto.domain.PassengerInfoDTO;
import lombok.Data;

/**
 * 列车购票出参
 */
@Data
public class TrainPurchaseTicketRespDTO {

    /**
     * 乘车人信息
     */
    private PassengerInfoDTO passengerInfo;

    /**
     * 车厢号
     */
    private String carriageNumber;

    /**
     * 座位号
     */
    private String seatNumber;

    /**
     * 座位金额
     */
    private Integer amount;
}
