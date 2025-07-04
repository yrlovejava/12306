package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.lang.Pair;
import com.alibaba.nacos.shaded.com.google.common.collect.Lists;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.AbstractTrainPurchaseTicketTemplate;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.select.SeatSelection;
import com.squirrel.index12306.biz.ticketservice.toolkit.CarriageVacantSeatCalculateUtil;
import com.squirrel.index12306.biz.ticketservice.toolkit.ChooseSeatUtil;
import com.squirrel.index12306.biz.ticketservice.toolkit.SeatNumberUtil;
import com.squirrel.index12306.biz.ticketservice.toolkit.SurplusNeedMatchSeatUtil;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.BitMapCheckSeat;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.BitMapCheckSeatStatusFactory;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_CARRIAGE_SEAT_STATUS;
import static com.squirrel.index12306.biz.ticketservice.service.handler.ticket.TrainBusinessClassPurchaseTicketHandler.deepCopy;
import static com.squirrel.index12306.biz.ticketservice.service.handler.ticket.TrainBusinessClassPurchaseTicketHandler.mergeArrays;
import static com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base.BitMapCheckSeatStatusFactory.TRAIN_FIRST;

/**
 * 高铁二等票购票组件
 */
@Component
@RequiredArgsConstructor
public class TrainSecondClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    private final DistributedCache distributedCache;
    private final SeatService seatService;

    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.SECOND_CLASS.getName();
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

        // 查询有余票的车厢集合
        List<String> trainCarriageList = seatService.listUsableCarriageNumber(trainId, requestParam.getSeatType(), departure, arrival);
        // 获取车厢余票
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(trainId, departure, arrival, trainCarriageList);

        int remainingTicketSum = trainStationCarriageRemainingTicket.stream().mapToInt(Integer::intValue).sum();
        if (remainingTicketSum < passengerSeatDetails.size()) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }

        // 如果用户选择了座位
        if(CollUtil.isNotEmpty(requestParam.getRequestParam().getChooseSeats())){
            return matchSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        }

        // 如果乘车人小于5
        if (passengerSeatDetails.size() < 5) {
            return this.selectSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        }

        // 多人乘车
        return this.selectComplexSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
    }

    /**
     * 选择两个及两个以下的座位
     *
     * @param requestParam                        选票参数
     * @param trainCarriageList                   列车车厢集合
     * @param trainStationCarriageRemainingTicket 车厢余票集合
     * @return 选择的座位详情
     */
    private List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam, List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
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

        // 记录各车厢剩余空闲座位的数量
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>(trainCarriageList.size());
        // 记录各车厢剩余空闲座位的布局
        Map<String, int[][]> actualSeatsMap = new HashMap<>(trainCarriageList.size());
        // 记录选择的位置
        Map<String, int[][]> carriagesNumberSeatsMap = new HashMap<>();

        // 尽量让一起买票的乘车人在一个车厢
        String carriagesNumber;
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            // 当前车厢剩余的票数
            int remainingTicket = trainStationCarriageRemainingTicket.get(i);
            if (remainingTicket > passengerSeatDetails.size()) {
                // 当前车厢号
                carriagesNumber = trainCarriageList.get(i);
                // 查询所有可用的座位（未选的座位）
                List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber, requestParam.getSeatType(), departure, arrival);
                int[][] actualSeats = new int[18][5];
                for (int j = 1; j < 19; j++) {
                    for (int k = 1; k < 6; k++) {
                        // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                        if (j <= 9) {
                            actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(2, k)) ? 0 : 1;
                        } else {
                            actualSeats[j - 1][k - 1] = listAvailableSeat.contains(j + SeatNumberUtil.convert(2, k)) ? 0 : 1;
                        }
                    }
                }
                // 先选择邻座的位置
                int[][] select = SeatSelection.adjacent(passengerSeatDetails.size(), actualSeats);

                // 如果选择到了，那么在map中记录
                if (select != null) {
                    carriagesNumberSeatsMap.put(carriagesNumber, select);
                    break;
                }
                // 计算当前车厢可选的位置数量
                int demotionStockNum = 0;
                for (int[] actualSeat : actualSeats) {
                    for (int seat : actualSeat) {
                        if (seat == 0) {
                            demotionStockNum++;
                        }
                    }
                }
                // 记录 车厢号 --> 可选座位数量
                demotionStockNumMap.putIfAbsent(carriagesNumber, demotionStockNum);
                // 记录 车厢号 --> 座位布局
                actualSeatsMap.putIfAbsent(carriagesNumber, actualSeats);
                // 先需要遍历所有的车厢，查找是否是否能邻座
                if (i < trainStationCarriageRemainingTicket.size() - 1) {
                    continue;
                }

                // 到达这里说明每个车厢都无法满足邻座
                // 如果邻座算法无法匹配，尝试对用户进行降级分配，同车厢不邻座
                for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                    // 车厢号
                    String carriageNumberBack = entry.getKey();
                    // 该车厢号剩余的空余座位
                    int demotionStockNumBack = entry.getValue();
                    // 选取空余座位数大于需要选择座位数的车厢
                    if (demotionStockNumBack > passengerSeatDetails.size()) {
                        // 获取到空闲座位布局
                        int[][] seats = actualSeatsMap.get(carriageNumberBack);
                        // 选择同车厢不临座的座位
                        int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                        if (Objects.equals(nonAdjacentSeats.length, passengerSeatDetails.size())) {
                            select = nonAdjacentSeats;
                            carriagesNumberSeatsMap.put(carriageNumberBack, select);
                            break;
                        }
                    }
                }

                // 如果同车厢也无法匹配，则对用户座位再次降级，不同车厢不邻座
                if (Objects.isNull(select)) {
                    // 需要的座位数量
                    int undistributedPassengerSize = passengerSeatDetails.size();
                    for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                        // 车厢号
                        String carriageNumberBack = entry.getKey();
                        // 空闲座位数量
                        int demotionStockNumBack = entry.getValue();
                        // 获取该车厢的座位布局
                        int[][] seats = actualSeatsMap.get(carriageNumberBack);
                        // 选择同车厢不邻座
                        int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(Math.min(undistributedPassengerSize, demotionStockNumBack), seats);
                        // 更新所需的座位数量
                        undistributedPassengerSize -= demotionStockNumBack;
                        // 记录可以选择的位置
                        carriagesNumberSeatsMap.put(entry.getKey(), nonAdjacentSeats);
                        if(undistributedPassengerSize <= 0){
                            break;
                        }
                    }
                }
            }
        }
        // 乘车人员在单一车厢座位不满足，触发乘车人员分布在不同车厢
        int count = (int) carriagesNumberSeatsMap.values().stream()
                .flatMap(Arrays::stream)
                .count();
        if (CollUtil.isNotEmpty(carriagesNumberSeatsMap) && passengerSeatDetails.size() == count) {
            int countNum = 0;
            for (Map.Entry<String, int[][]> entry : carriagesNumberSeatsMap.entrySet()) {
                List<String> selectSeats = new ArrayList<>();
                for (int[] ints : entry.getValue()) {
                    selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(2, ints[1]));
                }
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    // 乘车人信息
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum++);
                    result.setSeatNumber(selectSeat);// 座位号
                    result.setSeatType(currentTicketPassenger.getSeatType());// 座位类型
                    result.setCarriageNumber(entry.getKey());// 车厢号
                    result.setPassengerId(currentTicketPassenger.getPassengerId());// 乘客id
                    // 添加到返回结果中
                    actualResult.add(result);
                }
            }
        }
        return actualResult;
    }

    private List<TrainPurchaseTicketRespDTO> matchSeats(SelectSeatDTO requestParam, List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 1.基本信息
        String trainId = requestParam.getRequestParam().getTrainId();// 列车id
        String departure = requestParam.getRequestParam().getDeparture();// 出发站
        String arrival = requestParam.getRequestParam().getArrival();// 到达站
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();// 乘车人信息

        // 返回信息
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        // 每个车厢的空闲座位
        Map<String, PriorityQueue<List<Pair<Integer, Integer>>>> carriageNumberVacantSeat = new HashMap<>(16);
        // 用户选择的座位
        List<String> chooseSeatList = requestParam.getRequestParam().getChooseSeats();

        // 2.将用户选择的座位转换
        HashMap<Integer, Integer> convert = ChooseSeatUtil.convert(TRAIN_FIRST, chooseSeatList);
        // 获取BitMap检查用户座位是否还空闲的实例
        BitMapCheckSeat instance = BitMapCheckSeatStatusFactory.getInstance(TRAIN_FIRST);
        // 获取RedisTemplate
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();

        // 3.遍历车厢，获取确定选择的座位
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            // 车厢号
            String carriagesNumber = trainCarriageList.get(i);

            // 获取该车厢空余的座位
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber, requestParam.getSeatType(), departure, arrival);

            // 构建座位矩阵，将真实的座位状态和布局使用二维数组标识
            int[][] actualSeats = new int[18][5];
            for (int j = 1; j < 19; j++) {
                for (int k = 1; k < 6; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    if (j <= 9) {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(2, k)) ? 0 : 1;
                    } else {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains(j + SeatNumberUtil.convert(2, k)) ? 0 : 1;
                    }
                }
            }

            // 将座位矩阵转换为BitMap，将座位状态标识为1或0，方便之后检查
            String keySuffix = CacheUtil.buildKey(trainId, departure, arrival, carriagesNumber);
            String key = TRAIN_CARRIAGE_SEAT_STATUS + keySuffix;
            for (int i1 = 0; i1 < 18; i1++) {
                for (int j = 0; j < 5; j++) {
                    stringRedisTemplate.opsForValue()
                            .setBit(key, i1 * 5 + j, actualSeats[i1][j] == 0);
                }
            }

            // 使用bitmap检查用户选择的座位是否空闲
            boolean isExists = instance.checkSeat(key, convert, distributedCache);

            List<String> selectSeats = new ArrayList<>(passengerSeatDetails.size() + 1);
            final List<Pair<Integer, Integer>> sureSeatList = Lists.newArrayListWithCapacity(chooseSeatList.size());

            // 构建车厢的空闲座位队列
            PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatQueue = CarriageVacantSeatCalculateUtil
                    .buildCarriageVacantSeatList(actualSeats, 18, 5);

            // 计算当前车厢空余数量-乘车人数量。
            int seatCount = vacantSeatQueue.parallelStream()
                    .mapToInt(Collection::size).sum() - passengerSeatDetails.size();

            // 4.如果座位空闲，并且座位数量大于等于乘车人数，则选择座位
            if (isExists && seatCount >= 0) {
                // 确认座位
                convert.forEach((k, v) -> {
                    List<Pair<Integer, Integer>> temp = new ArrayList<>();
                    for (List<Pair<Integer, Integer>> next : vacantSeatQueue) {
                        Iterator<Pair<Integer, Integer>> pairIterator = next.iterator();
                        while (pairIterator.hasNext()) {
                            Pair<Integer, Integer> pair = pairIterator.next();
                            if (Objects.equals(pair.getValue(), k) && temp.size() < v) {
                                temp.add(pair);
                                pairIterator.remove();
                            }
                        }
                        if (temp.size() == v) {
                            sureSeatList.addAll(temp);
                            break;
                        }
                    }
                });

                // 如果确认的座位小于乘车人的数量，则补充座位
                if (sureSeatList.size() != passengerSeatDetails.size()) {
                    List<Pair<Integer, Integer>> pairList = vacantSeatQueue.parallelStream()
                            .flatMap(Collection::stream)
                            .toList();
                    int needSeatSize = passengerSeatDetails.size() - sureSeatList.size();
                    sureSeatList.addAll(pairList.subList(0, needSeatSize));
                }

                // 构建返回信息
                for (Pair<Integer, Integer> each : sureSeatList) {
                    selectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                }
                AtomicInteger countNum = new AtomicInteger(0);
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum.getAndIncrement());
                    result.setSeatNumber(selectSeat);
                    result.setSeatType(currentTicketPassenger.getSeatType());
                    result.setCarriageNumber(carriagesNumber);
                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                    actualResult.add(result);
                }

                // 返回结果
                return actualResult;
            } else {
                if (i < trainStationCarriageRemainingTicket.size()) {
                    carriageNumberVacantSeat.put(carriagesNumber, vacantSeatQueue);
                    if (i == trainStationCarriageRemainingTicket.size() - 1) {
                        List<Pair<Integer, Integer>> actualSureSeat = new ArrayList<>(chooseSeatList.size());
                        for (Map.Entry<String, PriorityQueue<List<Pair<Integer, Integer>>>> entry : carriageNumberVacantSeat.entrySet()) {
                            PriorityQueue<List<Pair<Integer, Integer>>> entryValue = entry.getValue();
                            int size = entryValue.parallelStream().mapToInt(Collection::size).sum();
                            if (size >= passengerSeatDetails.size()) {
                                actualSureSeat = SurplusNeedMatchSeatUtil.getSurplusNeedMatchSeat(passengerSeatDetails.size(), entryValue);
                                for (Pair<Integer, Integer> each : actualSureSeat) {
                                    selectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                                }
                                AtomicInteger countNum = new AtomicInteger(0);
                                for (String selectSeat : selectSeats) {
                                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum.getAndIncrement());
                                    result.setSeatNumber(selectSeat);
                                    result.setSeatType(currentTicketPassenger.getSeatType());
                                    result.setCarriageNumber(entry.getKey());
                                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                                    actualResult.add(result);
                                }
                                break;
                            }
                        }
                        if (CollUtil.isEmpty(actualSureSeat)) {
                            AtomicInteger countNum = new AtomicInteger(0);
                            for (Map.Entry<String, PriorityQueue<List<Pair<Integer, Integer>>>> entry : carriageNumberVacantSeat.entrySet()) {
                                PriorityQueue<List<Pair<Integer, Integer>>> entryValue = entry.getValue();
                                if (actualSureSeat.size() < passengerSeatDetails.size()) {
                                    List<Pair<Integer, Integer>> surplusNeedMatchSeat = SurplusNeedMatchSeatUtil.getSurplusNeedMatchSeat(passengerSeatDetails.size() - actualSureSeat.size(), entryValue);
                                    actualSureSeat.addAll(surplusNeedMatchSeat);
                                    List<String> actualSelectSeats = new ArrayList<>();
                                    for (Pair<Integer, Integer> each : surplusNeedMatchSeat) {
                                        actualSelectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(2, each.getValue() + 1));
                                    }
                                    for (String selectSeat : actualSelectSeats) {
                                        TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                                        PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum.getAndIncrement());
                                        result.setSeatNumber(selectSeat);
                                        result.setSeatType(currentTicketPassenger.getSeatType());
                                        result.setCarriageNumber(entry.getKey());
                                        result.setPassengerId(currentTicketPassenger.getPassengerId());
                                        actualResult.add(result);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return actualResult;
    }

    /**
     * 选择多人的座位
     *
     * @param requestParam                        选择座位参数
     * @param trainCarriageList                   车厢号集合
     * @param trainStationCarriageRemainingTicket 车厢余票集合
     * @return 选择的座位详情
     */
    private List<TrainPurchaseTicketRespDTO> selectComplexSeats(SelectSeatDTO requestParam, List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
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

        // 记录各车厢剩余空闲座位的数量
        Map<String, Integer> demotionStockNumMap = new LinkedHashMap<>(trainCarriageList.size());
        // 记录各车厢剩余空闲座位的布局
        Map<String, int[][]> actualSeatsMap = new HashMap<>(trainCarriageList.size());
        // 记录选择的位置
        Map<String, int[][]> carriagesNumberSeatsMap = new HashMap<>();

        // 尽量让一起买票的乘车人在一个车厢
        String carriagesNumber;
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            // 当前车厢号
            carriagesNumber = trainCarriageList.get(i);
            // 查询所有可用的座位（未选的座位）
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriagesNumber, requestParam.getSeatType(), departure, arrival);
            int[][] actualSeats = new int[18][5];
            for (int j = 1; j < 19; j++) {
                for (int k = 1; k < 6; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    if (j <= 9) {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(2, k)) ? 0 : 1;
                    } else {
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains(j + SeatNumberUtil.convert(2, k)) ? 0 : 1;
                    }
                }
            }
            // 复制一份真实的座位分布，用来标记之后的选择座位
            int[][] actualSeatsTrainScript = deepCopy(actualSeats);
            // 真正选择的位置
            List<int[][]> actualSelects = new ArrayList<>();

            // 按每组最多两人，将乘客信息切分为若干个小组
            List<List<PurchaseTicketPassengerDetailDTO>> splitPassengerSeatDetails = ListUtil.split(passengerSeatDetails, 2);
            for (List<PurchaseTicketPassengerDetailDTO> each : splitPassengerSeatDetails) {
                // 首先选择邻座
                int[][] select = SeatSelection.adjacent(each.size(), actualSeatsTrainScript);
                if (select != null) {
                    for (int[] ints : select) {
                        actualSeatsTrainScript[ints[0] - 1][ints[1] - 1] = 1;
                    }
                    actualSelects.add(select);
                }
            }

            // 如果实际选择的座位小组数量等于划分的小组数量，证明每个小组都能选择到座位
            if (actualSelects.size() == splitPassengerSeatDetails.size()) {
                int[][] actualSelect = null;
                for (int j = 0; j < actualSelects.size(); j++) {
                    if (j == 0) {
                        actualSelect = mergeArrays(actualSelects.get(j), actualSelects.get(j + 1));
                    }
                    if (j != 0 && actualSelects.size() > 2){
                        actualSelect = mergeArrays(actualSelect,actualSelects.get(i+1));
                    }
                }
                // 将结果保存到carriagesNumberSeatsMap 中
                carriagesNumberSeatsMap.put(carriagesNumber, actualSelect);
                // 跳出循环
                break;
            }

            // 走到这里代表邻座分配失败,计算该车厢剩余的座位数量
            int demotionStockNum = 0;
            for (int[] actualSeat : actualSeats) {
                for (int i1 : actualSeat) {
                    if (i1 == 0) {
                        demotionStockNum++;
                    }
                }
            }
            // 记录该车厢剩余的座位数量
            demotionStockNumMap.putIfAbsent(carriagesNumber, demotionStockNum);
            // 更新该车厢的实际选择座位
            actualSeatsMap.putIfAbsent(carriagesNumber, actualSeats);
        }

        // 到达这里说明每个车厢都无法满足邻座
        // 如果邻座算法无法匹配，尝试对用户进行降级分配，同车厢不邻座
        if (CollUtil.isEmpty(carriagesNumberSeatsMap)) {
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                // 车厢号
                String carriageNumberBack = entry.getKey();
                // 该车厢号剩余的空余座位
                int demotionStockNumBack = entry.getValue();
                // 选取空余座位数大于需要选择座位数的车厢
                if (demotionStockNumBack > passengerSeatDetails.size()) {
                    // 获取到空闲座位分布
                    int[][] seats = actualSeatsMap.get(carriageNumberBack);
                    // 选择不邻座座位
                    int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(passengerSeatDetails.size(), seats);
                    if (Objects.equals(nonAdjacentSeats.length, passengerSeatDetails.size())) {
                        carriagesNumberSeatsMap.put(carriageNumberBack, nonAdjacentSeats);
                        break;
                    }
                }
            }
        }


        // 如果同车厢也无法匹配，则对用户座位再次降级，不同车厢不邻座
        if (CollUtil.isEmpty(carriagesNumberSeatsMap)) {
            // 需要的座位数量
            int undistributedPassengerSize = passengerSeatDetails.size();
            for (Map.Entry<String, Integer> entry : demotionStockNumMap.entrySet()) {
                // 车厢号
                String carriageNumberBack = entry.getKey();
                // 空闲座位数量
                int demotionStockNumBack = entry.getValue();
                // 获取该车厢的座位布局
                int[][] seats = actualSeatsMap.get(carriageNumberBack);
                // 选择同车厢不邻座
                int[][] nonAdjacentSeats = SeatSelection.nonAdjacent(Math.min(undistributedPassengerSize, demotionStockNumBack), seats);
                // 更新所需的座位数量
                undistributedPassengerSize -= demotionStockNumBack;
                // 记录可以选择的位置
                carriagesNumberSeatsMap.put(entry.getKey(), nonAdjacentSeats);
                if(undistributedPassengerSize <= 0){
                    break;
                }
            }
        }

        // 乘车人员在单一车厢座位不满足，触发乘车人员分布在不同车厢
        int count = (int) carriagesNumberSeatsMap.values().stream()
                .flatMap(Arrays::stream)
                .count();
        if (CollUtil.isNotEmpty(carriagesNumberSeatsMap) && passengerSeatDetails.size() == count) {
            int countNum = 0;
            for (Map.Entry<String, int[][]> entry : carriagesNumberSeatsMap.entrySet()) {
                List<String> selectSeats = new ArrayList<>();
                for (int[] ints : entry.getValue()) {
                    selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(2, ints[1]));
                }
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    // 乘车人信息
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum++);
                    result.setSeatNumber(selectSeat);// 座位号
                    result.setSeatType(currentTicketPassenger.getSeatType());// 座位类型
                    result.setCarriageNumber(entry.getKey());// 车厢号
                    result.setPassengerId(currentTicketPassenger.getPassengerId());// 乘客id
                    actualResult.add(result);
                }
            }
        }

        return actualResult;
    }
}
