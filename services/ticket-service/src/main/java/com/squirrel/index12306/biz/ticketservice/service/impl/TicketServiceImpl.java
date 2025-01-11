package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.squirrel.index12306.biz.ticketservice.common.enums.*;
import com.squirrel.index12306.biz.ticketservice.dao.entity.*;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.*;
import com.squirrel.index12306.biz.ticketservice.dto.domain.*;
import com.squirrel.index12306.biz.ticketservice.dto.req.CancelTicketOrderReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.squirrel.index12306.biz.ticketservice.mq.event.DelayCloseOrderEvent;
import com.squirrel.index12306.biz.ticketservice.mq.producer.DelayCloseOrderSendProducer;
import com.squirrel.index12306.biz.ticketservice.remote.PayRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.PayInfoRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderItemCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.TicketService;
import com.squirrel.index12306.biz.ticketservice.service.cache.SeatMarginCacheLoader;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.select.TrainSeatTypeSelector;
import com.squirrel.index12306.biz.ticketservice.toolkit.DateUtil;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import com.squirrel.index12306.frameworks.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
public class TicketServiceImpl extends ServiceImpl<TicketMapper,TicketDO> implements TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);
    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final PayRemoteService payRemoteService;
    private final StationMapper stationMapper;
    private final DelayCloseOrderSendProducer delayCloseOrderSendProducer;
    private final TrainSeatTypeSelector trainSeatTypeSelector;
    private final SeatMarginCacheLoader seatMarginCacheLoader;
    private final AbstractChainContext<PurchaseTicketReqDTO> abstractChainContext;

    /**
     * 根据条件查询车票
     *
     * @param requestParam 分页查询条件
     * @return Result<IPage < TicketPageQueryRespDTO>>
     */
    @Override
    public TicketPageQueryRespDTO pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        // 查找出发站
        StationDO fromStationDO = stationMapper.selectOne(Wrappers.lambdaQuery(StationDO.class)
                .eq(StationDO::getCode, requestParam.getFromStation())
        );
        // 查找到达站
        StationDO toStation = stationMapper.selectOne(Wrappers.lambdaQuery(StationDO.class)
                .eq(StationDO::getCode, requestParam.getToStation())
        );
        // TODO 责任链模式 验证城市名称是否存在、不存在加载缓存等等
        // 通过mybatis-plus分页查询
        LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.<TrainStationRelationDO>lambdaQuery()
                .eq(TrainStationRelationDO::getStartRegion, fromStationDO.getName())
                .eq(TrainStationRelationDO::getEndRegion, toStation.getName());
        List<TrainStationRelationDO> trainStationRelationList = trainStationRelationMapper.selectList(queryWrapper);
        // 车次信息集合
        List<TicketListDTO> seatResults = new ArrayList<>();
        // 列车标签集合
        Set<Integer> trainBrandSet = new HashSet<>();
        // 处理车站信息
        for (TrainStationRelationDO each : trainStationRelationList) {
            // 查询列车信息
            TrainDO trainDO = trainMapper.selectOne(Wrappers.<TrainDO>lambdaQuery()
                    .eq(TrainDO::getId, each.getTrainId()));
            // 封装返回 DTO
            TicketListDTO result = new TicketListDTO();
            result.setTrainId(String.valueOf(trainDO.getId()));// 列车ID
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
            if (StrUtil.isNotBlank(trainDO.getTrainTag())) {
                result.setTrainTags(StrUtil.split(trainDO.getTrainTag(), ","));
            }
            // 出发到到达需要的天数
            long betweenDay = cn.hutool.core.date.DateUtil.betweenDay(each.getDepartureTime(), each.getArrivalTime(), true);
            result.setDaysArrived((int) betweenDay);
            result.setSaleStatus(new Date().after(trainDO.getSaleTime()) ? 0 : 1);// 销售状态
            result.setSaleTime(trainDO.getSaleTime()); // 可售时间
            if (StrUtil.isNotBlank(trainDO.getTrainBrand())) {
                trainBrandSet.addAll(StrUtil.split(trainDO.getTrainBrand(), ",")
                        .stream()
                        .map(Integer::parseInt)
                        .toList());
            }
            // 构建查询条件
            LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                    .eq(TrainStationPriceDO::getDeparture, each.getDeparture())// 出发站一样
                    .eq(TrainStationPriceDO::getArrival, each.getArrival())// 到达站一样
                    .eq(TrainStationPriceDO::getTrainId, each.getTrainId());// 列车id也一样
            // 查询动车座位票价
            List<TrainStationPriceDO> trainStationPriceDOList = trainStationPriceMapper.selectList(trainStationPriceQueryWrapper);
            // 座位类型集合
            List<SeatClassDTO> seatClassList = new ArrayList<>();
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            trainStationPriceDOList.forEach(item -> {
                // 获取座位类型
                String seatType = String.valueOf(item.getSeatType());
                // 获取key后缀
                String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                // 获取该座位类型的票数量
                Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, seatType);
                Integer quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            // 从缓存中获取，如果没有从数据库中查询并添加到redis中
                            Map<String, String> seatMarginMap = seatMarginCacheLoader.load(
                                    String.valueOf(each.getTrainId()),
                                    seatType,
                                    item.getDeparture(),
                                    item.getArrival()
                            );
                            return Optional.ofNullable(seatMarginMap.get(String.valueOf(item.getSeatType()))).map(Integer::parseInt).orElse(0);
                        });
                SeatClassDTO seatClassDTO = SeatClassDTO.builder()
                        .type(item.getSeatType())// 席别类型
                        .price(new BigDecimal(item.getPrice()).divide(new BigDecimal("100"), 1, RoundingMode.HALF_UP))// 票价
                        .quantity(quantity)// 数量
                        .candidate(false)// 是否候补
                        .build();
                seatClassList.add(seatClassDTO);
            });
            result.setSeatClassList(seatClassList);
            seatResults.add(result);
        }
        return TicketPageQueryRespDTO.builder()
                .trainList(seatResults)// 车次集合
                .departureStationList(buildDepartureStationList(seatResults))// 出发站集合
                .arrivalStationList(buildArrivalStationList(seatResults))// 终点站集合
                .trainBrandList(trainBrandSet.stream().toList())// 列车标签集合
                .seatClassTypeList(buildSeatClassList(seatResults))// 座位类型集合
                .build();
    }

    /**
     * 取消车票订单
     *
     * @param requestParam 取消车票订单入参
     */
    @Override
    public void cancelTicketOrder(CancelTicketOrderReqDTO requestParam) {
        ticketOrderRemoteService.cancelTicketOrder(requestParam);
    }

    /**
     * 获取列车车次信息中的出发站
     *
     * @param seatResults 车次信息集合
     * @return 出发站集合
     */
    private List<String> buildDepartureStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getDeparture).distinct().collect(Collectors.toList());
    }

    /**
     * 获取列车车次信息中的到达站
     *
     * @param seatResults 车次信息集合
     * @return 到达站集合
     */
    private List<String> buildArrivalStationList(List<TicketListDTO> seatResults) {
        return seatResults.stream().map(TicketListDTO::getArrival).distinct().collect(Collectors.toList());
    }

    /**
     * 获取列车车次信息中的座位类型
     *
     * @param seatResults 车次信息集合
     * @return 座位类型集合
     */
    private List<Integer> buildSeatClassList(List<TicketListDTO> seatResults) {
        Set<Integer> resultSeatClassList = new HashSet<>();
        for (TicketListDTO each : seatResults) {
            for (SeatClassDTO item : each.getSeatClassList()) {
                resultSeatClassList.add(item.getType());
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
    @Transactional(rollbackFor = Throwable.class)
    public TicketPurchaseRespDTO purchaseTickets(PurchaseTicketReqDTO requestParam) {
        // 责任链模式，验证 0:参数必填 1:参数正确性 2:列车车次余量是否充足 3:乘客是否已买当前车次等
        abstractChainContext.handler(TicketChainMarkEnum.TRAIN_PURCHASE_TICKET_FILTER.name(),requestParam);
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
        List<TrainPurchaseTicketRespDTO> trainPurchaseTicketResults = trainSeatTypeSelector.select(trainDO.getTrainType(), requestParam);
        // 批量插入车票到数据库
        List<TicketDO> ticketDOList = trainPurchaseTicketResults.stream()
                .map(each -> TicketDO.builder()
                        .username(UserContext.getUsername())
                        .trainId(Long.parseLong(requestParam.getTrainId()))
                        .carriageNumber(each.getCarriageNumber())
                        .seatNumber(each.getSeatNumber())
                        .passengerId(each.getPassengerId())
                        .ticketStatus(TicketStatusEnum.UNPAID.getCode())
                        .build())
                .toList();
        saveBatch(ticketDOList);

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
                    .userId(UserContext.getUserId())
                    .username(UserContext.getUsername())
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                    .build();
            // 远程调用创建订单
            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            if(!ticketOrderResult.isSuccess() || StrUtil.isBlank(ticketOrderResult.getData())){
                log.error("订单服务调用失败，返回结果：{}", ticketOrderResult.getMessage());
                throw new ServiceException("订单服务调用失败");
            }
            // 发送 RocketMQ 延时消息，指定时间后取消订单
            delayCloseOrderSendProducer.sendMessage(new DelayCloseOrderEvent(ticketOrderResult.getData()));
        } catch (Throwable ex) {
            log.error("远程调用订单服务创建错误，请求参数: {}", JSON.toJSONString(requestParam), ex);
            // TODO 回退锁定车票
            throw ex;
        }
        if (!ticketOrderResult.isSuccess()) {
            log.error("远程调用订单服务创建失败，请求参数: {}", JSON.toJSONString(requestParam));
            // TODO 回退锁定车票
            throw new ServiceException(ticketOrderResult.getMessage());
        }
        return new TicketPurchaseRespDTO(ticketOrderResult.getData(), ticketOrderDetailResults);
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
