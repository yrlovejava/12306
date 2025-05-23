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
import com.squirrel.index12306.biz.ticketservice.toolkit.base.BitMapCheckSeat;
import com.squirrel.index12306.biz.ticketservice.toolkit.base.BitMapCheckSeatStatusFactory;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_CARRIAGE_SEAT_STATUS;
import static com.squirrel.index12306.biz.ticketservice.toolkit.base.BitMapCheckSeatStatusFactory.TRAIN_BUSINESS;

/**
 * 高铁商务座购票组件
 */
@Component
@RequiredArgsConstructor
public class TrainBusinessClassPurchaseTicketHandler extends AbstractTrainPurchaseTicketTemplate {

    private final DistributedCache distributedCache;
    private final SeatService seatService;

    @Override
    public String mark() {
        return VehicleTypeEnum.HIGH_SPEED_RAIN.getName() + VehicleSeatTypeEnum.BUSINESS_CLASS.getName();
    }

    /**
     * 选择座位
     * 1.同车厢邻座
     * 2.同车厢不邻座
     * 3.不同车厢不邻座
     *
     * @param requestParam 购票请求入参
     * @return 购票信息的集合
     */
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

        // 查询有余票的车厢号集合
        List<String> trainCarriageList = seatService.listUsableCarriageNumber(trainId, requestParam.getSeatType(),departure,arrival);
        // 获取车厢余票
        List<Integer> trainStationCarriageRemainingTicket = seatService.listSeatRemainingTicket(trainId, departure, arrival, trainCarriageList);

        // 计算总共的剩余票数
        int remainingTicketSum = trainStationCarriageRemainingTicket.stream().mapToInt(Integer::intValue).sum();
        if (remainingTicketSum < passengerSeatDetails.size()) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其他站点");
        }
        if (passengerSeatDetails.size() < 3) {
            // 如果用户选择了座位
            if(CollUtil.isNotEmpty(requestParam.getRequestParam().getChooseSeats())){
                return matchSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
            }
            return this.selectSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
        }
        return this.selectComplexSeats(requestParam, trainCarriageList, trainStationCarriageRemainingTicket);
    }

    /**
     * 匹配高铁商务座座位
     * @param requestParam 选座位参数
     * @param trainCarriageList 列车车厢号集合
     * @param trainStationCarriageRemainingTicket 列车车厢余票集合
     * @return 列车票购买信息
     */
    private List<TrainPurchaseTicketRespDTO> matchSeats(SelectSeatDTO requestParam, List<String> trainCarriageList, List<Integer> trainStationCarriageRemainingTicket) {
        // 1.基本信息
        // 列车id
        String trainId = requestParam.getRequestParam().getTrainId();
        // 出发站
        String departure = requestParam.getRequestParam().getDeparture();
        // 到达站
        String arrival = requestParam.getRequestParam().getArrival();
        // 乘车人信息
        List<PurchaseTicketPassengerDetailDTO> passengerSeatDetails = requestParam.getPassengerSeatDetails();

        // 2.初始化乘车人具体购买信息（返回值）
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        // 3.初始化座位信息（车厢号->空闲座位分布）
        Map<String, PriorityQueue<List<Pair<Integer, Integer>>>> carriageNumberVacantSeat = new HashMap<>(4);

        // 4.遍历车厢，查询可选的座位
        for (int i = 0; i < trainStationCarriageRemainingTicket.size(); i++) {
            // 车厢号
            String carriageNumber = trainCarriageList.get(i);
            // 查询可选座位
            List<String> listAvailableSeat = seatService.listAvailableSeat(trainId, carriageNumber, requestParam.getSeatType(), departure, arrival);
            // 生成实际的座位分布
            int[][] actualSeats = new int[2][3];
            for (int j = 1; j < 3; j++) {
                for (int k = 1; k < 4; k++) {
                    actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(0, k))? 0 : 1;
                }
            }
            // 获取用户选择的座位
            List<String> chooseSeatList = requestParam.getRequestParam().getChooseSeats();

            // 将实际的座位状态存储到Redis位图中，方便后续检查座位是否存在
            String keySuffix = CacheUtil.buildKey(trainId, departure, arrival, carriageNumber);
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            String key = TRAIN_CARRIAGE_SEAT_STATUS + keySuffix;
            for (int i1 = 0; i1 < 2; i1++) {
                for (int j = 0; j < 3; j++) {
                    stringRedisTemplate.opsForValue()
                            .setBit(key, i1 * 3 + j, actualSeats[i1][j] == 0);
                }
            }

            // 将用户选择的座位转换成（座位号->数量）
            HashMap<Integer, Integer> convert = ChooseSeatUtil.convert(TRAIN_BUSINESS, chooseSeatList);
            // 使用位图来判断是否存在
            BitMapCheckSeat instance = BitMapCheckSeatStatusFactory.getInstance(TRAIN_BUSINESS);
            boolean isExists = instance.checkSeat(key, convert, distributedCache);

            // 当前车厢要选择的座位
            List<String> selectSeats = new ArrayList<>(passengerSeatDetails.size() + 1);
            final List<Pair<Integer, Integer>> sureSeatList = Lists.newArrayListWithCapacity(chooseSeatList.size());

            // 统计连续空余的座位(这里是连续的座位为一个集合)
            PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatList = CarriageVacantSeatCalculateUtil
                    .buildCarriageVacantSeatList(actualSeats, 2, 3);
            // 计算所有空余的座位数量
            int seatCount = vacantSeatList.parallelStream()
                    .mapToInt(Collection::size).sum() - passengerSeatDetails.size();

            List<Pair<Integer, Integer>> otherPair = new ArrayList<>(16);
            if(isExists && seatCount >= 0){
                // 座位类型存在 且 座位数量大于等于购买的座位数量
                // 确认座位
                convert.forEach((k,v) -> {
                    List<Pair<Integer, Integer>> temp = new ArrayList<>();
                    for (List<Pair<Integer, Integer>> pair : vacantSeatList) {
                        for (Pair<Integer, Integer> each : pair) {
                            if (Objects.equals(each.getValue(), k) && temp.size() < v) {
                                temp.add(each);
                            } else {
                                otherPair.add(each);
                            }
                        }
                        if (temp.size() == v) {
                            sureSeatList.addAll(temp);
                            break;
                        }
                    }
                });
                // 如果确认的座位不等于乘车人的数量
                if(sureSeatList.size() != passengerSeatDetails.size()){
                    int needSeatSize = passengerSeatDetails.size() - sureSeatList.size();
                    sureSeatList.addAll(otherPair.subList(0, needSeatSize));
                }
                // 在选择的座位集合中添加
                for (Pair<Integer, Integer> each : sureSeatList) {
                    selectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(0, each.getValue() + 1));
                }
                // 封装返回值
                AtomicInteger countNum = new AtomicInteger(0);
                for (String selectSeat : selectSeats) {
                    TrainPurchaseTicketRespDTO result = new TrainPurchaseTicketRespDTO();
                    PurchaseTicketPassengerDetailDTO currentTicketPassenger = passengerSeatDetails.get(countNum.getAndIncrement());
                    result.setSeatNumber(selectSeat);
                    result.setSeatType(currentTicketPassenger.getSeatType());
                    result.setCarriageNumber(carriageNumber);
                    result.setPassengerId(currentTicketPassenger.getPassengerId());
                    actualResult.add(result);
                }
                return actualResult;
            } else {
                if(i < trainStationCarriageRemainingTicket.size()){
                    carriageNumberVacantSeat.put(carriageNumber,vacantSeatList);
                    if(i == trainStationCarriageRemainingTicket.size() - 1){
                        // 如果是最后一个车厢
                        List<Pair<Integer,Integer>> actualSureSeat = new ArrayList<>(chooseSeatList.size());
                        for (Map.Entry<String, PriorityQueue<List<Pair<Integer, Integer>>>> entry : carriageNumberVacantSeat.entrySet()) {
                            PriorityQueue<List<Pair<Integer, Integer>>> entryValue = entry.getValue();
                            // 并行计算当前遍历到的车厢的座位数量
                            int size = entryValue.parallelStream()
                                    .mapToInt(Collection::size).sum();
                            if(size >= passengerSeatDetails.size()){
                                actualSureSeat = SurplusNeedMatchSeatUtil.getSurplusNeedMatchSeat(passengerSeatDetails.size(),entryValue);
                                for (Pair<Integer, Integer> each : actualSureSeat) {
                                    selectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(0, each.getValue() + 1));
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
                        if(CollUtil.isEmpty(actualSureSeat)){
                            AtomicInteger countNum = new AtomicInteger(0);
                            for (Map.Entry<String, PriorityQueue<List<Pair<Integer, Integer>>>> entry : carriageNumberVacantSeat.entrySet()) {
                                PriorityQueue<List<Pair<Integer, Integer>>> entryValue = entry.getValue();
                                if (actualSureSeat.size() < passengerSeatDetails.size()) {
                                    List<Pair<Integer, Integer>> surplusNeedMatchSeat = SurplusNeedMatchSeatUtil.getSurplusNeedMatchSeat(passengerSeatDetails.size() - actualSureSeat.size(), entryValue);
                                    actualSureSeat.addAll(surplusNeedMatchSeat);
                                    List<String> actualSelectSeats = new ArrayList<>();
                                    for (Pair<Integer, Integer> each : surplusNeedMatchSeat) {
                                        actualSelectSeats.add("0" + (each.getKey() + 1) + SeatNumberUtil.convert(0, each.getValue() + 1));
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
                int[][] actualSeats = new int[2][3];
                for (int j = 1; j < 3; j++) {
                    for (int k = 1; k < 4; k++) {
                        // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                        actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(0, k)) ? 0 : 1;
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
                    selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(0, ints[1]));
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
            int[][] actualSeats = new int[2][3];
            for (int j = 1; j < 3; j++) {
                for (int k = 1; k < 4; k++) {
                    // 当前默认按照复兴号商务座排序，后续这里需要按照简单工厂对车类型进行获取 y 轴
                    actualSeats[j - 1][k - 1] = listAvailableSeat.contains("0" + j + SeatNumberUtil.convert(0, k)) ? 0 : 1;
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
                    selectSeats.add("0" + ints[0] + SeatNumberUtil.convert(0, ints[1]));
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

    public static int[][] mergeArrays(int[][] array1, int[][] array2) {
        List<int[]> list = new ArrayList<>(Arrays.asList(array1));
        list.addAll(Arrays.asList(array2));
        return list.toArray(new int[0][]);
    }

    public static int[][] deepCopy(int[][] originalArray) {
        int[][] copy = new int[originalArray.length][originalArray[0].length];
        for (int i = 0; i < originalArray.length; i++) {
            System.arraycopy(originalArray[i], 0, copy[i], 0, originalArray[i].length);
        }
        return copy;
    }
}
