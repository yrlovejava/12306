package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    private final AbstractStrategyChoose abstractStrategyChoose;
    private final TicketMapper ticketMapper;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final PayRemoteService payRemoteService;

    /**
     * 根据条件查询车票
     *
     * @param requestParam 分页查询条件
     * @return Result<IPage < TicketPageQueryRespDTO>>
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
                // 获取key后缀
                String keySuffix = StrUtil.join("_", each.getTrainId(), item.getDeparture(), item.getArrival());
                // 获取该座位类型的票数量
                Object quantityObj = stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(item.getSeatType()));
                Integer quantity = Optional.ofNullable(quantityObj)
                        .map(Object::toString)
                        .map(Integer::parseInt)
                        .orElseGet(() -> {
                            // TODO 后续适配缓存失效逻辑
                            return 0;
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
                    .username(UserContext.getUsername())
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                    .build();
            // 远程调用创建订单
            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
            // 发送 RocketMQ 延时消息，指定时间后取消订单
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
