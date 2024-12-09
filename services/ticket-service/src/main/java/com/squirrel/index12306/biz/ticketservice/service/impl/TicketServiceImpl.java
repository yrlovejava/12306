package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationPriceDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationPriceMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.BulletTrainDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TicketService;
import com.squirrel.index12306.biz.ticketservice.toolkit.DateUtil;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.page.PageResponse;
import com.squirrel.index12306.framework.starter.database.toolkit.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 车票接口实现
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TrainMapper trainMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final TrainStationPriceMapper trainStationPriceMapper;
    private final DistributedCache distributedCache;

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
}
