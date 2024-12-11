package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import com.squirrel.index12306.biz.ticketservice.common.constant.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.constant.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PassengerInfoDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.CarriageService;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 高铁商务座购票组件
 */
@Component
@RequiredArgsConstructor
public class TrainBusinessPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    private final CarriageService carriageService;
    private final DistributedCache distributedCache;
    private final SeatService seatService;

    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.BUSINESS_CLASS.getName();
    }

    @Override
    protected List<TrainPurchaseTicketRespDTO> selectSeats(PurchaseTicketReqDTO requestParam) {
        List<String> passengerIds = requestParam.getPassengerIds();
        // 判断哪个车厢有座位。获取对应座位类型的车厢号集合，依次进行判断数据是否有余票
        List<String> trainCarriageList = carriageService.listCarriageNumber(requestParam.getTrainId(), requestParam.getSeatType());
        // 获取车厢余票
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(), trainCarriageList);
        // 尽量让一起买票的乘车人在一个车厢
        String carriagesNumber;
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        for (int i = 0;i < trainStationCarriageRemainingTicket.size();i++){
            int remainingTicket = trainStationCarriageRemainingTicket.get(i);
            if (remainingTicket > passengerIds.size()) {
                carriagesNumber = trainCarriageList.get(i);
                List<String> listAvailableSeat = seatService.listAvailableSeat(requestParam.getTrainId(), carriagesNumber);
                List<String> selectSeats = selectSeats(listAvailableSeat, passengerIds.size());
                for (int j = 0;j < selectSeats.size();j++) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    String seatNumber = selectSeats.get(j);
                    result.setSeatNumber(seatNumber);
                    result.setCarriageNumber(carriagesNumber);
                    result.setPassengerInfo(new PassengerInfoDTO().setPassengerId(passengerIds.get(j)));
                    actualResult.add(result);
                }
            }
            break;
        }
        // TODO 如果一个车厢不满足乘客人数，需要进行拆分
        // TODO 扣减车厢余票缓存，扣减站点余票缓存
        return actualResult;
    }

    /**
     * 寻找可选座位
     * @param availableSeats 可选的位置
     * @param requiredPassengers 需要的数量
     * @return 可选的座位
     */
    private static List<String> selectSeats(List<String> availableSeats, int requiredPassengers) {
        List<String> selectedSeats = new ArrayList<>();
        for (String seat : availableSeats) {
            if (selectedSeats.size() >= requiredPassengers) {
                break;
            }
            selectedSeats.add(seat);
        }
        return selectedSeats;
    }
}
