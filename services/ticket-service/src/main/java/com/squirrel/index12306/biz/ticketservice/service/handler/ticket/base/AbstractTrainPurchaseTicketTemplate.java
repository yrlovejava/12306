package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.UserRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.toolkit.StationCalculateUtil;
import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import com.squirrel.index12306.framework.starter.bases.constant.UserConstant;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import com.squirrel.index12306.frameworks.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum.*;

/**
 * 抽象高铁购票模板基础服务
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTrainPurchaseTicketTemplate implements ApplicationRunner, IPurchaseTicket, AbstractExecuteStrategy<PurchaseTicketReqDTO, List<TrainPurchaseTicketRespDTO>> {

    private SeatMapper seatMapper;
    private TrainStationMapper trainStationMapper;
    private UserRemoteService userRemoteService;
    private TrainStationPriceMapper trainStationPriceMapper;

    /**
     * 执行策略，这里是购票策略
     * @param requestParam 购票请求参数
     * @return List<TrainPurchaseTicketRespDTO> 乘车人和对应的座位
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TrainPurchaseTicketRespDTO> executeResp(PurchaseTicketReqDTO requestParam) {
        // TODO 后续逻辑全部转换为 LUA 缓存原子操作
        // 选择座位，有子类具体实现
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeats(requestParam);
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
        try{
            // 查询乘车人信息
            passengerRemoteResult = userRemoteService.listPassengerQueryByIds(
                    UserContext.getUsername(), passengerIds);
            if(passengerRemoteResult.isSuccess() && CollUtil.isNotEmpty(passengerRemoteResultList = passengerRemoteResult.getData())) {
                // 选择座位的时候，PassengerInfo 中只有乘客id，这里需要给每一个乘车人赋值剩余信息
                actualResult.forEach(each -> {
                    String passengerId = each.getPassengerId();
                    passengerRemoteResultList.stream()
                            .filter(item -> Objects.equals(item.getId(),passengerId))
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
        }catch (Throwable ex) {
            log.error("用户服务远程调用查询乘车人相关信息错误",ex);
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
            SeatDO updateSeatDO = SeatDO.builder().seatStatus(LOCKED.getCode()).build();
            seatMapper.update(updateSeatDO, updateWrapper);
        }));
        return actualResult;
    }

    /**
     * 选择座位
     *
     * @param requestParam 购票请求入参
     * @return 乘车人座位
     */
    protected abstract List<TrainPurchaseTicketRespDTO> selectSeats(PurchaseTicketReqDTO requestParam);

    /**
     * 应用启动后执行初始化任务
     * @param args 参数
     * @throws Exception 可能抛出异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        seatMapper = ApplicationContextHolder.getBean(SeatMapper.class);
        trainStationMapper = ApplicationContextHolder.getBean(TrainStationMapper.class);
        userRemoteService = ApplicationContextHolder.getBean(UserRemoteService.class);
        trainStationPriceMapper = ApplicationContextHolder.getBean(TrainStationPriceMapper.class);
    }
}