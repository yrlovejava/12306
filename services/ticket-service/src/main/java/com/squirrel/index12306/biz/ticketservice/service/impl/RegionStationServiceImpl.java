package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant;
import com.squirrel.index12306.biz.ticketservice.common.enums.RegionStationQueryTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.RegionDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.StationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.StationMapper;
import com.squirrel.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.StationQueryRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.RegionStationService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.enums.FlagEnum;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;

/**
 * 地区以及车站接口实现层
 */
@Service
@RequiredArgsConstructor
public class RegionStationServiceImpl implements RegionStationService {

    private final RegionMapper regionMapper;
    private final StationMapper stationMapper;
    private final DistributedCache distributedCache;

    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&城市查询参数
     * @return 车站&城市站点集合信息
     */
    @Override
    public List<RegionStationQueryRespDTO> listRegionStation(RegionStationQueryReqDTO requestParam) {
        // TODO 请求缓存
        if (StrUtil.isNotBlank(requestParam.getName())) {
            // 构造查询参数
            LambdaQueryWrapper<StationDO> queryWrapper = Wrappers.lambdaQuery(StationDO.class)
                    .likeRight(StationDO::getName, requestParam.getName())// 右模糊匹配名字
                    .or()
                    .likeRight(StationDO::getSpell, requestParam.getName());// 右模糊匹配拼音
            List<StationDO> stationDOList = stationMapper.selectList(queryWrapper);
            return BeanUtil.convert(stationDOList, RegionStationQueryRespDTO.class);
        }
        // TODO 请求缓存
        LambdaQueryWrapper<RegionDO> queryWrapper = switch (requestParam.getQueryType()) {
            case 0 -> Wrappers.lambdaQuery(RegionDO.class)
                    .eq(RegionDO::getPopularFlag, FlagEnum.TRUE.code());
            case 1 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.A_E.getSpells());
            case 2 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.F_J.getSpells());
            case 3 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.K_O.getSpells());
            case 4 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.P_T.getSpells());
            case 5 -> Wrappers.lambdaQuery(RegionDO.class)
                    .in(RegionDO::getInitial, RegionStationQueryTypeEnum.U_Z.getSpells());
            default -> throw new ClientException("查询失败，请检查查询参数是否正确");
        };
        List<RegionDO> regionDOList = regionMapper.selectList(queryWrapper);
        return BeanUtil.convert(regionDOList, RegionStationQueryRespDTO.class);
    }

    /**
     * 查询所有车站&城市站点集合信息
     *
     * @return 车站返回数据集合
     */
    @Override
    public List<StationQueryRespDTO> listAllStation() {
        return distributedCache.get(
                RedisKeyConstant.STATION_ALL,
                List.class,
                () -> BeanUtil.convert(stationMapper.selectList(Wrappers.emptyWrapper()),StationQueryRespDTO.class),
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS
        );
    }
}
