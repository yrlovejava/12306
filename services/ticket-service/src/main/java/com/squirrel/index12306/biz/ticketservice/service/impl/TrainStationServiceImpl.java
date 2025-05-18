package com.squirrel.index12306.biz.ticketservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.biz.ticketservice.toolkit.StationCalculateUtil;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 列车站点接口实现层
 */
@Service
@RequiredArgsConstructor
public class TrainStationServiceImpl implements TrainStationService {

    private final TrainStationMapper trainStationMapper;

    /**
     * 根据列车 ID 查询站点信息
     *
     * @param trainId 列车 ID
     * @return 列车经停站信息
     */
    @Override
    public List<TrainStationQueryRespDTO> listTrainStationQuery(String trainId) {
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId));
        return BeanUtil.convert(trainStationDOList,TrainStationQueryRespDTO.class);
    }

    /**
     * 计算列车站点路线关系
     * 获取开始站点和目的站点及中间站点信息
     *
     * @param trainId   列车 ID
     * @param departure 出发站
     * @param arrival   到达站
     * @return 列车站点路线关系信息
     */
    @Override
    public List<RouteDTO> listTrainStationRoute(String trainId, String departure, String arrival) {
        // 查询所有站点
        List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(Wrappers.lambdaQuery(TrainStationDO.class)
                .eq(TrainStationDO::getTrainId, trainId)
                .select(TrainStationDO::getDeparture)
        );
        // 解析站点信息
        List<String> trainStationAllList = trainStationDOList.stream()
                .map(TrainStationDO::getDeparture)
                .toList();
        return StationCalculateUtil.throughStation(trainStationAllList,departure,arrival);
    }
}
