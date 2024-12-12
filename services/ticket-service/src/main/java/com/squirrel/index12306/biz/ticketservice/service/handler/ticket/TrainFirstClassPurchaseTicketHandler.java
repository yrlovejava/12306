package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;

import java.util.List;

/**
 * 高铁一等票购票组件
 */
public class TrainFirstClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.FIRST_CLASS.getName();
    }

    @Override
    protected List<TrainPurchaseTicketRespDTO> selectSeats(PurchaseTicketReqDTO requestParam) {
        return null;
    }
}
