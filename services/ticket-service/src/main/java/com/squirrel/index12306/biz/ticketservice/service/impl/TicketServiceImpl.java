package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.constant.enums.TicketStatusEnum;
import com.squirrel.index12306.biz.ticketservice.common.constant.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.constant.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TicketDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TicketMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.BulletTrainDTO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PassengerInfoDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderItemCreateRemoteReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.TicketService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.toolkit.DateUtil;
import com.squirrel.index12306.framework.starter.bases.constant.UserConstant;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.page.PageResponse;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.database.toolkit.PageUtil;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractStrategyChoose;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

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

    /**
     * 根据条件查询车票
     * @param requestParam 分页查询条件
     * @return Result<IPage<TicketPageQueryRespDTO>>
     */
    @Override
    public PageResponse<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam) {
        // TODO 责任链模式 验证城市名称是否存在、不存在加载缓存等等
        // 通过mybatis-plus分页查询
        LambdaQueryWrapper<TrainStationRelationDO> queryWrapper = Wrappers.<TrainStationRelationDO>lambdaQuery()
                .eq(TrainStationRelationDO::getStartRegion, requestParam.getFromStation())
                .eq(TrainStationRelationDO::getEndRegion, requestParam.getToStation());
        IPage<TrainStationRelationDO> trainStationRelationPage = trainStationRelationMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        return PageUtil.convert(trainStationRelationPage,each -> {
            // 查询列车信息
            TrainDO trainDO = trainMapper.selectOne(Wrappers.<TrainDO>lambdaQuery()
                    .eq(TrainDO::getId, each.getTrainId()));
            // 封装返回 DTO
            TicketPageQueryRespDTO result = new TicketPageQueryRespDTO();
            result.setTrainNumber(trainDO.getTrainNumber()); // 车次
            result.setDepartureTime(each.getDepartureTime()); // 出发时间
            result.setArrivalTime(each.getArrivalTime()); // 到达时间
            result.setDuration(DateUtil.calculateHourDifference(each.getDepartureTime(), each.getArrivalTime())); // 等待间隔
            result.setDeparture(each.getDeparture()); // 出发站
            result.setArrival(each.getArrival()); // 到达站
            result.setDepartureFlag(each.getDepartureFlag());// 出发标识
            result.setArrivalFlag(each.getArrivalFlag()); // 到达标识
            // 如果trainType 等于 0，也就是列车为高铁
            if (Objects.equals(trainDO.getTrainType(),0)){
                BulletTrainDTO bulletTrainDTO = new BulletTrainDTO();
                // 构建查询条件
                LambdaQueryWrapper<TrainStationPriceDO> trainStationPriceQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                        .eq(TrainStationPriceDO::getDeparture, each.getDeparture())// 出发站一样
                        .eq(TrainStationPriceDO::getArrival, each.getArrival())// 到达站一样
                        .eq(TrainStationPriceDO::getTrainId, each.getTrainId());// 列车id也一样
                // 查询高铁座位票价
                List<TrainStationPriceDO> trainStationPriceDOList = trainStationPriceMapper.selectList(trainStationPriceQueryWrapper);
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 设置高铁座位票价
                trainStationPriceDOList.forEach(item -> {
                    // Redis中车票数量key: KeyPrefix + 列车ID_起始站点_终点
                    String keySuffix = StrUtil.join("_",each.getTrainId(),item.getDeparture(),item.getArrival());
                    switch (item.getSeatType()) {
                        case 0:
                            // 获取商务座票数量
                            String businessClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "0");
                            // 设置商务座数量
                            bulletTrainDTO.setBusinessClassQuantity(Integer.parseInt(businessClassQuantity));
                            bulletTrainDTO.setBusinessClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setBusinessClassCandidate(false);
                            break;
                        case 1:
                            // 获取一等座票数量
                            String firstClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "1");
                            // 设置一等座票数量
                            bulletTrainDTO.setFirstClassQuantity(Integer.parseInt(firstClassQuantity));
                            bulletTrainDTO.setFirstClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setBusinessClassCandidate(false);
                            break;
                        case 2:
                            // 获取一等座票数量
                            String secondClassQuantity = (String) stringRedisTemplate.opsForHash().get(TRAIN_STATION_REMAINING_TICKET + keySuffix, "2");
                            // 设置一等座票数量
                            bulletTrainDTO.setFirstClassQuantity(Integer.parseInt(secondClassQuantity));
                            bulletTrainDTO.setSecondClassPrice(item.getPrice());
                            // TODO 候补逻辑后续补充
                            bulletTrainDTO.setBusinessClassCandidate(false);
                            break;
                        default:
                            break;
                    }
                });
                result.setBulletTrain(bulletTrainDTO);
            }
            return result;
        });
    }

    /**
     * 购买车票
     *
     * @param requestParam 车票购买请求参数
     * @return 订单号
     */
    @Override
    public String purchaseTickets(PurchaseTicketReqDTO requestParam) {
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
                VehicleTypeEnum.findNameByCode(trainDO.getTrainType()) + VehicleSeatTypeEnum.findNameByCode(requestParam.getSeatType()),
                requestParam);
        // TODO 批量插入
        trainPurchaseTicketResults.forEach(each -> {
            PassengerInfoDTO passengerInfo = each.getPassengerInfo();
            TicketDO ticketDO = new TicketDO();
            // TODO 创建用户上下文
            ticketDO.setUsername(MDC.get(UserConstant.USER_NAME_KEY));
            ticketDO.setTrainId(Long.parseLong(requestParam.getTrainId()));
            ticketDO.setCarriageNumber(each.getCarriageNumber());
            ticketDO.setSeatNumber(each.getSeatNumber());
            ticketDO.setPassengerId(passengerInfo.getPassengerId());
            ticketDO.setTicketStatus(TicketStatusEnum.UNPAID.getCode());
            ticketMapper.insert(ticketDO);
        });
        Result<String> ticketOrderResult;
        try {
            List<TicketOrderItemCreateRemoteReqDTO> orderItemCreateRemoteReqDTOList = new ArrayList<>();
            trainPurchaseTicketResults.forEach(each -> {
                PassengerInfoDTO passengerInfo = each.getPassengerInfo();
                // 构造创建订单明细请求参数
                TicketOrderItemCreateRemoteReqDTO orderItemCreateRemoteReqDTO = TicketOrderItemCreateRemoteReqDTO.builder()
                        .amount(each.getAmount())// 座位金额
                        .carriageNumber(each.getCarriageNumber())// 车厢号
                        .seatNumber(each.getSeatNumber())// 座位号
                        .idCard(passengerInfo.getIdCard())// 证件号
                        .idType(passengerInfo.getIdType())// 证件类型
                        .phone(passengerInfo.getPhone())// 手机号
                        .realName(passengerInfo.getRealName())// 真实姓名
                        .build();
                // 加入订单创建请求列表中
                orderItemCreateRemoteReqDTOList.add(orderItemCreateRemoteReqDTO);
            });
            // 构造创建整个订单请求参数
            TicketOrderCreateRemoteReqDTO orderCreateRemoteReqDTO = TicketOrderCreateRemoteReqDTO.builder()
                    .departure(requestParam.getDeparture())
                    .arrival(requestParam.getArrival())
                    .orderTime(new Date())
                    // TODO 创建用户上下文
                    .username(MDC.get(UserConstant.USER_NAME_KEY))
                    .trainId(Long.parseLong(requestParam.getTrainId()))
                    .ticketOrderItems(orderItemCreateRemoteReqDTOList)
                    .build();
            // 远程调用创建订单
            ticketOrderResult = ticketOrderRemoteService.createTicketOrder(orderCreateRemoteReqDTO);
        }catch (Throwable ex) {
            log.error("远程调用订单服务创建错误，请求参数: {}", JSON.toJSONString(requestParam),ex);
            // TODO 回退锁定车票
            throw ex;
        }
        if (ticketOrderResult == null || !ticketOrderResult.isSuccess()) {
            log.error("远程调用订单服务创建失败，请求参数: {}",JSON.toJSONString(requestParam));
            // TODO 回退锁定车票
            throw new ServiceException(ticketOrderResult.getMessage());
        }
        return ticketOrderResult.getData();
    }
}
