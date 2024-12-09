package com.squirrel.index12306.biz.ticketservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TrainStationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
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
}
