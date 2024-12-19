package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.CarriageService;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

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
        // 获取乘车人信息
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // 判断哪个车厢有座位。获取对应座位类型的车厢号集合，依次进行判断数据是否有余票
        List<String> trainCarriageList = carriageService.listCarriageNumber(requestParam.getTrainId(), requestParam.getPassengers().get(0).getSeatType());
        // 获取车厢余票
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival(), trainCarriageList);
        // 尽量让一起买票的乘车人在一个车厢
        String carriagesNumber;
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        for (int i = 0;i < trainStationCarriageRemainingTicket.size();i++){
            // 当前车厢剩余的票数
            int remainingTicket = trainStationCarriageRemainingTicket.get(i);
            if (remainingTicket > passengerDetails.size()) {
                carriagesNumber = trainCarriageList.get(i);
                // 查询所有可用的座位（未选的座位）
                List<String> listAvailableSeat = seatService.listAvailableSeat(requestParam.getTrainId(), carriagesNumber);
                // 在未选的座位中寻找可选的座位
                List<String> selectSeats = selectSeats(listAvailableSeat, passengerDetails.size());
                for (int j = 0;j < selectSeats.size();j++) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    String seatNumber = selectSeats.get(j);
                    // TODO 席位
                    result.setSeatType(0);
                    // TODO 用户类型
                    result.setUserType(0);
                    result.setSeatNumber(seatNumber);// 座位号
                    result.setCarriageNumber(carriagesNumber);// 车厢号
                    result.setPassengerId(passengerDetails.get(j).getPassengerId());// 乘车人id
                    actualResult.add(result);
                }
                break;
            }
        }
        // TODO 如果一个车厢不满足乘客人数，需要进行拆分
        // TODO 扣减车厢余票缓存，扣减站点余票缓存
        if(CollUtil.isNotEmpty(actualResult)){
            // 获取key后缀 列车ID_出发站_到达站
            String keySuffix = StrUtil.join("_", requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
            // 在Redis中扣除该车次对应座位类型的余票
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            stringRedisTemplate.opsForHash().increment(
                    TRAIN_STATION_REMAINING_TICKET + keySuffix,
                    String.valueOf(requestParam.getPassengers().get(0).getSeatType()),
                    -passengerDetails.size());
        }
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
