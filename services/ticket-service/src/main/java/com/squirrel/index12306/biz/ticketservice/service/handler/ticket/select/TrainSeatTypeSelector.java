package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleSeatTypeEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.UserRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.toolkit.StationCalculateUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractStrategyChoose;
import com.squirrel.index12306.frameworks.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 购票时列车座位选择器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TrainSeatTypeSelector {

    private final SeatMapper seatMapper;
    private final TrainStationMapper trainStationMapper;
    private final UserRemoteService userRemoteService;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final AbstractStrategyChoose abstractStrategyChoose;

    /**
     * 选择座位
     * @param trainType 列车类型
     * @param requestParam 购票请求参数
     * @return 列车购票出参
     */
    public List<TrainPurchaseTicketRespDTO> select(Integer trainType, PurchaseTicketReqDTO requestParam) {
        // 获取乘车人信息
        List<PurchaseTicketPassengerDetailDTO> passengerDetails = requestParam.getPassengers();
        // 如果多个乘车人选择了不同座位，需要拆分处理
        Map<Integer, List<PurchaseTicketPassengerDetailDTO>> seatTypeMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType));
        // 返回结果
        List<TrainPurchaseTicketRespDTO> actualResult = new ArrayList<>();
        seatTypeMap.forEach((seatType, passengerSeatDetails) -> {
            // 构造策略的mark
            String buildStrategyKey = VehicleTypeEnum.findNameByCode(trainType) + VehicleSeatTypeEnum.findNameByCode(seatType);
            // 构造选座位的参数
            SelectSeatDTO selectSeatDTO = SelectSeatDTO.builder()
                    .seatType(seatType)
                    .passengerSeatDetails(passengerSeatDetails)
                    .requestParam(requestParam)
                    .build();
            // 策略模式选座位
            List<TrainPurchaseTicketRespDTO> aggregationResult = abstractStrategyChoose.chooseAndExecuteResp(buildStrategyKey, selectSeatDTO);
            // 在返回结果中添加
            actualResult.addAll(aggregationResult);
        });
        if (CollUtil.isEmpty(actualResult)) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
        }
        // 查询车站出发站-终点站座位价格
        LambdaQueryWrapper<TrainStationPriceDO> lambdaQueryWrapper = Wrappers.lambdaQuery(TrainStationPriceDO.class)
                .eq(TrainStationPriceDO::getTrainId, requestParam.getTrainId())
                .eq(TrainStationPriceDO::getDeparture, requestParam.getDeparture())
                .eq(TrainStationPriceDO::getArrival, requestParam.getArrival())
                .eq(TrainStationPriceDO::getSeatType, requestParam.getPassengers().get(0).getSeatType());
        TrainStationPriceDO trainStationPriceDO = trainStationPriceMapper.selectOne(lambdaQueryWrapper);
        // 获取乘车人的id集合
        List<String> passengerIds = actualResult.stream()
                .map(TrainPurchaseTicketRespDTO::getPassengerId)
                .toList();
        Result<List<PassengerRespDTO>> passengerRemoteResult;
        List<PassengerRespDTO> passengerRemoteResultList;
        try {
            // 查询乘车人信息
            passengerRemoteResult = userRemoteService.listPassengerQueryByIds(
                    UserContext.getUsername(), passengerIds);
            if (passengerRemoteResult.isSuccess() && CollUtil.isNotEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                // 选择座位的时候，PassengerInfo 中只有乘客id，这里需要给每一个乘车人赋值剩余信息
                actualResult.forEach(each -> {
                    String passengerId = each.getPassengerId();
                    passengerRemoteResultList.stream()
                            .filter(item -> Objects.equals(item.getId(), passengerId))
                            .findFirst()
                            .ifPresent(passenger -> {
                                each.setIdCard(passenger.getIdCard());// 证件号
                                each.setPhone(passenger.getPhone());// 手机号
                                each.setSeatType(passenger.getDiscountType());// 席别类型
                                each.setIdType(passenger.getIdType());// 证件类型
                                each.setRealName(passenger.getRealName());// 真实姓名
                            });
                    each.setAmount(trainStationPriceDO.getPrice());
                });
            }
        } catch (Throwable ex) {
            log.error("用户服务远程调用查询乘车人相关信息错误", ex);
            throw ex;
        }
        // 获取扣减开始站点和目的站点及中间站点信息
        // 查询列车的所有站点
        LambdaQueryWrapper<TrainStationDO> queryWrapper = Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, requestParam.getTrainId());
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(queryWrapper);
        List<String> trainStationAllList = trainStationDOList.stream().map(TrainStationDO::getDeparture).collect(Collectors.toList());
        // 计算所有的路线
        List<RouteDTO> routeList = StationCalculateUtil.throughStation(trainStationAllList, requestParam.getDeparture(), requestParam.getArrival());
        // 锁定座位车票库存
        actualResult.forEach(each -> routeList.forEach(item -> {
            LambdaUpdateWrapper<SeatDO> updateWrapper = Wrappers.lambdaUpdate(SeatDO.class)
                    .eq(SeatDO::getTrainId, requestParam.getTrainId())// 列车id
                    .eq(SeatDO::getCarriageNumber, each.getCarriageNumber())// 车厢号
                    .eq(SeatDO::getStartStation, item.getStartStation())// 路线的
                    .eq(SeatDO::getEndStation, item.getEndStation())
                    .eq(SeatDO::getSeatNumber, each.getSeatNumber());
            SeatDO updateSeatDO = SeatDO.builder().seatStatus(SeatStatusEnum.LOCKED.getCode()).build();
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
        return actualResult;
    }
}
