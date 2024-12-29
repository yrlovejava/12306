package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.service.CarriageService;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
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
public class TrainBusinessClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    private final CarriageService carriageService;
    private final DistributedCache distributedCache;
    private final SeatService seatService;

    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.BUSINESS_CLASS.getName();
    }

    @Override
    protected List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam) {
        // 列车id
        String trainId = requestParam.getRequestParam().getTrainId();
        // 出发站
        String departure = requestParam.getRequestParam().getDeparture();
        // 到达站
        String arrival = requestParam.getRequestParam().getArrival();
        // 乘车人信息
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();
        // 返回结果
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();

        // 判断哪个车厢有座位。获取对应座位类型的车厢号集合，依次进行判断数据是否有余票
        List<String> trainCarriageList = carriageService.listCarriageNumber(trainId, requestParam.getSeatType());
        // 获取车厢余票
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(trainId, departure, arrival, trainCarriageList);
        // 尽量让一起买票的乘车人在一个车厢
        String carriagesNumber;
        for (int i = 0;i < trainStationCarriageRemainingTicket.size();i++){
            // 当前车厢剩余的票数
            int remainingTicket = trainStationCarriageRemainingTicket.get(i);
            if (remainingTicket > passengerSeatDetails.size()) {
                carriagesNumber = trainCarriageList.get(i);
                // 查询所有可用的座位（未选的座位）
                List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber);
                // 在未选的座位中寻找可选的座位
                List<String> selectSeats = selectSeats(listAvailableSeat, passengerSeatDetails.size());
                for (int j = 0;j < selectSeats.size();j++) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    String seatNumber = selectSeats.get(j);
                    // 获取乘车人信息
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(j);
                    result.setSeatType(currentTicketPassenger.getSeatType());// 席别类型
                    result.setSeatNumber(seatNumber);// 座位号
                    result.setCarriageNumber(carriagesNumber);// 车厢号
                    result.setPassengerId(currentTicketPassenger.getPassengerId());// 乘车人id
                    actualResult.add(result);
                }
                break;
            }
        }
        // TODO 如果一个车厢不满足乘客人数，需要进行拆分
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
