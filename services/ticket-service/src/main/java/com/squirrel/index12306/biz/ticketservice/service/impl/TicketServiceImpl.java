package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.enums.*;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TicketDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TicketMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.*;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.PayRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderItemCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.TicketService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.toolkit.DateUtil;
import com.squirrel.index12306.framework.starter.bases.constant.UserConstant;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractStrategyChoose;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_INFO;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 车票接口实现
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final TicketMapper ticketMapper;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final PayRemoteService payRemoteService;

    /**
     * 根据条件查询车票
     * @param requestParam 分页查询条件
     * @return Result<IPage<TicketPageQueryRespDTO>>
     */
    @Override
    public TicketPageQueryRespDTO pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        // TODO 责任链模式 验证城市名称是否存在、不存在加载缓存等等
        // 通过mybatis-plus分页查询
        LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.<TrainStationRelationDO>lambdaQuery()
                .eq(TrainStationRelationDO::getStartRegion, requestParam.getFromStation())
                .eq(TrainStationRelationDO::getEndRegion, requestParam.getToStation());
        List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper.selectList(queryWrapper);
        // 车次信息集合
        List<TicketListDTO> seatResults = new ArrayList<>();
        Set<String> trainBrandSet = new HashSet<>();
        for (TrainStationRelationDO each : trainStationRelationList) {
            // 查询列车信息
            TrainDO trainDO = trainMapper.selectOne(Wrappers.<TrainDO>lambdaQuery()
                    .eq(TrainDO::getId, each.getTrainId()));
            // 封装返回 DTO
            TicketListDTO result = new TicketListDTO();
            result.setTrainNumber(trainDO.getTrainNumber()); // 车次
            result.setDepartureTime(each.getDepartureTime()); // 出发时间
            result.setArrivalTime(each.getArrivalTime()); // 到达时间
            result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime())); // 等待间隔
            result.setDeparture(each.getDeparture()); // 出发站
            result.setArrival(each.getArrival()); // 到达站
            result.setDepartureFlag(each.getDepartureFlag());// 出发标识
            result.setArrivalFlag(each.getArrivalFlag()); // 到达标识
            result.setTrainType(trainDO.getTrainType());// 列车类型
            // 列车标签集合
            if(StrUtil.isNotBlank(trainDO.getTrainTag())) {
                result.setTrainTag(StrUtil.split(trainDO.getTrainTag(),","));
            }
            // 出发到到达需要的天数
            long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(each.getDepartureTime(),each.getArrivalTime(),true);
            result.setDaysArrived((int)betweenDay);
            result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);// 销售状态
            result.setSaleTime(trainDO.getSaleTime()); // 可售时间
            if (StrUtil.isNotBlank(trainDO.getTrainBrand())) {
                trainBrandSet.addAll(TrainBrandEnum.findNameByCode(StrUtil.split(trainDO.getTrainBrand(), ",")));
            }
            // 构建查询条件
            LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getDeparture, each.getDeparture())// 出发站一样
                    .eq(TrainStationPriceDO::getArrival, each.getArrival())// 到达站一样
                    .eq(TrainStationPriceDO::getTrainId, each.getTrainId());// 列车id也一样
            // 查询动车座位票价
            List<TrainStationPriceDO> trainStationPriceDOList = trainStationPriceMapper.selectList(trainStationPriceQueryWrapper);
            // 如果trainType 等于 0，也就是列车为动车
            if (Objects.equals(trainDO.getTrainType(), 0)) {
                HighSpeedTrainDTO highSpeedTrainDTO = new HighSpeedTrainDTO();
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 设置动车座位票价
                trainStationPriceDOList.forEach(item -> {
                    // Redis中车票数量key: KeyPrefix + 列车ID_起始站点_终点
                    String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                    switch (item.getSeatType()) {
                        case 0 -> {
                            // 获取商务座票数量
                            String businessClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "0");
                            // 设置商务座数量
                            highSpeedTrainDTO.setBusinessClassQuantity(Integer.parseInt(businessClassQuantity));
                            highSpeedTrainDTO.setBusinessClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            highSpeedTrainDTO.setBusinessClassCandidate(false);
                        }
                        case 1 -> {
                            // 获取一等座票数量
                            String firstClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "1");
                            // 设置一等座票数量
                            highSpeedTrainDTO.setFirstClassQuantity(Integer.parseInt(firstClassQuantity));
                            highSpeedTrainDTO.setFirstClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            highSpeedTrainDTO.setBusinessClassCandidate(false);
                        }
                        case 2 -> {
                            // 获取一等座票数量
                            String secondClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "2");
                            // 设置一等座票数量
                            highSpeedTrainDTO.setFirstClassQuantity(Integer.parseInt(secondClassQuantity));
                            highSpeedTrainDTO.setSecondClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            highSpeedTrainDTO.setBusinessClassCandidate(false);
                        }
                        default -> {}
                    }
                });
                result.setHighSpeedTrain(highSpeedTrainDTO);
                seatResults.add(result);
            }
            // 如果trainType为1，那就是高铁
            else if (Objects.equals(trainDO.getTrainType(),1)) {
                BulletTrainDTO bulletTrainDTO = new BulletTrainDTO();
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                trainStationPriceDOList.forEach(item -> {
                    // 生成key后缀
                    String keySuffix = StrUtil.join("_",each.getTrainId(),item.getDeparture(),item.getArrival());
                    switch (item.getSeatType()) {
                        case 3 -> {
                            String secondClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "3");
                            bulletTrainDTO.setSecondClassQuantity(Integer.parseInt(secondClassQuantity));
                            bulletTrainDTO.setSecondClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setSecondSleeperCandidate(false);
                        }
                        case 4 -> {
                            String firstSleeperQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "4");
                            bulletTrainDTO.setFirstSleeperQuantity(Integer.parseInt(firstSleeperQuantity));
                            bulletTrainDTO.setFirstSleeperPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setFirstSleeperCandidate(false);
                        }
                        case 5 -> {
                            String secondSleeperQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "5");
                            bulletTrainDTO.setSecondSleeperQuantity(Integer.parseInt(secondSleeperQuantity));
                            bulletTrainDTO.setSecondSleeperPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setSecondSleeperCandidate(false);
                        }
                        default -> {}
                    }
                });
                result.setBulletTrain(bulletTrainDTO);
                seatResults.add(result);
            }
            // 如果trainType为2，那就是普通车
            else if (Objects.equals(trainDO.getTrainType(),2)) {
                RegularTrainDTO regularTrainDTO = new RegularTrainDTO();
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                trainStationPriceDOList.forEach(item -> {
                    String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                    switch (item.getSeatType()) {
                        case 6 -> {
                            String secondClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "3");
                            regularTrainDTO.setSoftSleeperQuantity(Integer.parseInt(secondClassQuantity));
                            regularTrainDTO.setSoftSleeperPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            regularTrainDTO.setSoftSleeperCandidate(false);
                        }
                        case 7 -> {
                            String firstSleeperQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "4");
                            regularTrainDTO.setHardSleeperQuantity(Integer.parseInt(firstSleeperQuantity));
                            regularTrainDTO.setHardSleeperPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            regularTrainDTO.setHardSleeperCandidate(false);
                        }
                        case 8 -> {
                            String secondSleeperQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "5");
                            regularTrainDTO.setHardSeatQuantity(Integer.parseInt(secondSleeperQuantity));
                            regularTrainDTO.setHardSeatPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            regularTrainDTO.setHardSeatCandidate(false);
                        }
                        default -> {
                        }
                    }
                    result.setRegularTrain(regularTrainDTO);
                    seatResults.add(result);
                });
            }
        }
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)
                .departureStationList(buildDepartureStationList(seatResults))
                .arrivalStationList(buildArrivalStationList(seatResults))
                .trainBrandList(trainBrandSet.stream().toList())
                .seatClassList(buildSeatClassList(seatResults))
                .build();
    }

    /**
     * 获取列车车次信息中的出发站
     * @param seatResults 车次信息集合
     * @return 出发站集合
     */
    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().collect(Collectors.toList());
    }

    /**
     * 获取列车车次信息中的到达站
     * @param seatResults 车次信息集合
     * @return 到达站集合
     */
    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    /**
     * 获取列车车次信息中的座位类型
     * @param seatResults 车次信息集合
     * @return 座位类型集合
     */
    private List<String> buildSeatClassList(List<TicketListDTO> seatResults) {
        Set<String> resultSeatClassList = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            // 动车座位类型
            HighSpeedTrainDTO highSpeedTrain = each.getHighSpeedTrain();
            if (highSpeedTrain != null) {
                Optional.ofNullable(highSpeedTrain.getBusinessClassPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.BUSINESS_CLASS.getValue()));
                Optional.ofNullable(highSpeedTrain.getFirstClassPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.FIRST_CLASS.getValue()));
                Optional.ofNullable(highSpeedTrain.getSecondClassQuantity()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.SECOND_CLASS.getValue()));
            }
            // 高铁座位类型
            BulletTrainDTO bulletTrain = each.getBulletTrain();
            if (bulletTrain != null) {
                Optional.ofNullable(bulletTrain.getFirstSleeperPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.FIRST_SLEEPER.getValue()));
                Optional.ofNullable(bulletTrain.getSecondSleeperPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.SECOND_SLEEPER.getValue()));
                Optional.ofNullable(bulletTrain.getSecondClassPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.SECOND_CLASS_CABIN_SEAT.getValue()));
                Optional.ofNullable(bulletTrain.getNoSeatPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getValue()));
            }
            // 普通车座位类型
            RegularTrainDTO regularTrain = each.getRegularTrain();
            if(regularTrain != null){
                Optional.ofNullable(regularTrain.getSoftSleeperPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.SOFT_SLEEPER.getValue()));
                Optional.ofNullable(regularTrain.getHardSleeperPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.HARD_SLEEPER.getValue()));
                Optional.ofNullable(regularTrain.getHardSeatPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.HARD_SEAT.getValue()));
                Optional.ofNullable(regularTrain.getNoSeatPrice()).ifPresent(item -> resultSeatClassList.add(VehicleSeatTypeEnum.NO_SEAT_SLEEPER.getValue()));
            }
        }
        return resultSeatClassList.stream().toList();
    }

    /**
     * 购买车票
     *
     * @param requestParam 车票购买请求参数
     * @return 订单号
     */
    @Override
    public TicketPurchaseRespDTO purchaseTickets(PurchaseTicketReqDTO requestParam) {
        String trainId = requestParam.getTrainId();
        // 在 redis 中查询列车信息
        TrainDO trainDO = distributedCache.get(
                TRAIN_INFO + trainId,
                TrainDO.class,
                () -> trainMapper.selectById(trainId),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
        // 使用策略模式购票
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults =
                abstractStrategyChoose.chooseAndExecuteResp(
                VehicleTypeEnum.findNameByCode(trainDO.getTrainType()) +
                        VehicleSeatTypeEnum.findNameByCode(requestParam.getPassengers().get(0).getSeatType()),
                requestParam);
        // TODO 批量插入
        trainPurchaseTicketResults.forEach(each -> {
            TicketDO ticketDO = new TicketDO();
            // TODO 创建用户上下文
            ticketDO.setUsername(MDC.get(UserConstant.USER_NAME_KEY));
            ticketDO.setTrainId(Long.parseLong(requestParam.getTrainId()));
            ticketDO.setCarriageNumber(each.getCarriageNumber());
            ticketDO.setSeatNumber(each.getSeatNumber());
            ticketDO.setPassengerId(each.getPassengerId());
            ticketDO.setTicketStatus(TicketStatusEnum.UNPAID.getCode());
            ticketMapper.insert(ticketDO);
        });
        // 返回的结果
        Result<String> ticketOrderResult;
        // 订单详情集合
        List<TicketOrderDetailRespDTO> ticketOrderDetailResults = new ArrayList<>();
        try {
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();
            trainPurchaseTicketResults.forEach(each -> {
                // 构造创建订单明细请求参数
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO.builder()
                        .amount(each.getAmount())// 座位金额
                        .carriageNumber(each.getCarriageNumber())// 车厢号
                        .seatNumber(each.getSeatNumber())// 座位号
                        .idCard(each.getIdCard())// 证件号
                        .idType(each.getIdType())// 证件类型
                        .phone(each.getPhone())// 手机号
                        .realName(each.getRealName())// 真实姓名
                        .build();
                // 构造订单明细返回参数
                TicketOrderDetailRespDTO ticketOrderDetailRespDTO = TicketOrderDetailRespDTO.builder()
                        .amount(each.getAmount())// 座位金额
                        .carriageNumber(each.getCarriageNumber())// 车厢号
                        .seatNumber(each.getSeatNumber())// 座位号
                        .idCard(each.getIdCard())// 证件号
                        .idType(each.getIdType())// 证件类型
                        .seatType(each.getSeatType())// 席别类型
                        .ticketType(each.getUserType())// 车票类型0：成人 1：儿童 2：学生 3：残疾军人
                        .realName(each.getRealName())// 真实姓名
                        .build();
                // 加入订单创建请求列表中
                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);
                ticketOrderDetailResults.add(ticketOrderDetailRespDTO);
            });
            // 构造创建整个订单请求参数
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderTime(new Date())
                    .source(SourceEnum.INTERNET.getCode())
                    // TODO 创建用户上下文
                    .username(MDC.get(UserConstant.USER_NAME_KEY))
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                    .build();
            // 远程调用创建订单
            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            // 发送 RocketMQ 延时消息，指定时间后取消订单
        }catch (Throwable ex) {
            log.error("远程调用订单服务创建错误，请求参数: {}", JSON.toJSONString(requestParam),ex);
            // TODO 回退锁定车票
            throw ex;
        }
        if (!ticketOrderResult.isSuccess()) {
            log.error("远程调用订单服务创建失败，请求参数: {}",JSON.toJSONString(requestParam));
            // TODO 回退锁定车票
            throw new ServiceException(ticketOrderResult.getMessage());
        }
        return new TicketPurchaseRespDTO(ticketOrderResult.getData(),ticketOrderDetailResults);
    }

    /**
     * 支付单详情查询
     *
     * @param orderSn 订单号
     * @return 支付单详情
     */
    @Override
    public PayInfoRespDTO getPayInfo(String orderSn) {
        return payRemoteService.getPayInfo(orderSn).getData();
    }
}
