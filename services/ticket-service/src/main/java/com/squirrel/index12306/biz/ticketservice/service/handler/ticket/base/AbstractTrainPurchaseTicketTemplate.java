package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.biz.ticketservice.toolkit.StationCalculateUtil;
import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.enums.SeatStatusEnum.*;

/**
 * 抽象高铁购票模板基础服务
 */
@RequiredArgsConstructor
public abstract class AbstractTrainPurchaseTicketTemplate implements ApplicationRunner, IPurchaseTicket, AbstractExecuteStrategy<PurchaseTicketReqDTO, List<TrainPurchaseTicketRespDTO>> {

    private SeatMapper seatMapper;
    private TrainStationMapper trainStationMapper;
    private DistributedCache distributedCache;

    /**
     * 执行策略，这里是购票策略
     * @param requestParam 购票请求参数
     * @return List<TrainPurchaseTicketRespDTO> 乘车人和对应的座位
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<TrainPurchaseTicketRespDTO> executeResp(PurchaseTicketReqDTO requestParam) {
        // TODO 后续逻辑全部转换为 LUA 缓存原子操作
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeats(requestParam);
        if (CollUtil.isEmpty(actualResult)) {
            throw new ServiceException("站点余票不足，请尝试更换座位类型或选择其它站点");
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
        distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);
    }
}