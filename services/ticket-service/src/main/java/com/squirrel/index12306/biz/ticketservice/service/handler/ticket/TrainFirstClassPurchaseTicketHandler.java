package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.service.CarriageService;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.select.SeatSelection;
import com.squirrel.index12306.biz.ticketservice.toolkit.SeatNumberUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 高铁一等票购票组件
 */
@Component
@RequiredArgsConstructor
public class TrainFirstClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    private final CarriageService carriageService;
    private final SeatService seatService;

    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.FIRST_CLASS.getName();
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
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>(passengerSeatDetails.size());

        // 查询有余票的车厢集合
        List<String> trainCarriageList = seatService.listUsableCarriageNumber(trainId, requestParam.getSeatType(), departure, arrival);
        // 获取车厢余票
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(trainId, departure, arrival, trainCarriageList);

        // 记录各车厢剩余空闲座位的数量
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>(trainCarriageList.size());
        // 记录各车厢剩余空闲座位的布局
        Map<String, int[][]> actualSeatsMap = new HashMap<>(trainCarriageList.size());

        // 尽量让一起买票的乘车人在一个车厢
        String carriagesNumber;
        for (int i = 0;i < trainStationCarriageRemainingTicket.size();i++){
            // 当前车厢剩余的票数
            int remainingTicket = trainStationCarriageRemainingTicket.get(i);
            if (remainingTicket > passengerSeatDetails.size()) {
                carriagesNumber = trainCarriageList.get(i);
                // 查询所有可用的座位（未选的座位）
                List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber,requestParam.getSeatType(),departure,arrival);
                int[][] actualSeats = new int[7][4];
                for (int j = 1; j < 8; j++) {
                    for (int k = 1; k < 5; k++) {
                        // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(1, k)) ? 0 : 1;
                    }
                }
                // 在未选的座位中寻找可选的座位
                List<String> selectSeats = new ArrayList<>();
                // 先选择邻座的位置
                int[][] select = SeatSelection.adjacent(passengerSeatDetails.size(), actualSeats);
                // 如果select为Null，证明当前车厢没选到完全邻座的位置
                if (Objects.isNull(select)) {
                    // 计算当前车厢可选的位置数量
                    int demotionStockNum = 0;
                    for(int[] actualSeat : actualSeats) {
                        for(int seat : actualSeat){
                            if(seat == 0){
                                demotionStockNum++;
                            }
                        }
                    }
                    // 记录 车厢号 --> 可选座位数量
                    demotionStockNumMap.putIfAbsent(carriagesNumber,demotionStockNum);
                    // 记录 车厢号 --> 座位布局
                    actualSeatsMap.putIfAbsent(carriagesNumber,actualSeats);
                    // 先需要遍历所有的车厢，查找是否是否能邻座
                    if(i < trainStationCarriageRemainingTicket.size() - 1) {
                        continue;
                    }

                    // 到达这里说明每个车厢都无法满足邻座
                    // 如果邻座算法无法匹配，尝试对用户进行降级分配，同车厢不邻座
                    for(Map.Entry<String,Integer> entry : demotionStockNumMap.entrySet()){
                        // 车厢号
                        String carriageNumberBack = entry.getKey();
                        // 该车厢号剩余的空余座位
                        int demotionStockNUmBack = entry.getValue();
                        // 选取空余座位数大于需要选择座位数的车厢
                        if(demotionStockNUmBack > passengerSeatDetails.size()){
                            // 获取到空闲座位布局
                            int[][] seats = actualSeatsMap.get(carriageNumberBack);
                            // 选择同车厢不临座的座位
                            int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                            if(Objects.equals(nonAdjacentSeats.length,passengerSeatDetails.size())) {
                                select = nonAdjacentSeats;
                                carriagesNumber= carriageNumberBack;
                                break;
                            }
                        }
                    }

                    // 如果到了这里，证明没有一个车厢空闲座位数满足需求
                    // TODO 如果同车厢也无法匹配，则对用户座位再次降级，不同车厢不邻座
                }

                // 处理选择到的座位，构造返回结果
                if(select != null){
                    for(int[] gets : select){
                        selectSeats.add("0" + gets[0] + SeatNumberUtil.convert(1, gets[1]));
                    }
                    for (int j = 0; j < selectSeats.size(); j++) {
                        TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                        // 座位号
                        String seatNumber = selectSeats.get(j);
                        // 乘车人信息
                        PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(j);
                        result.setSeatNumber(seatNumber);// 座位号
                        result.setSeatType(currentTicketPassenger.getSeatType());// 座位类型
                        result.setCarriageNumber(carriagesNumber);// 车厢号
                        result.setPassengerId(currentTicketPassenger.getPassengerId());// 乘客id
                        actualResult.add(result);
                    }
                    break;
                }
            }
        }
        // TODO 如果一个车厢不满足乘客人数，需要进行拆分
        return actualResult;
    }
}
