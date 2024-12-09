package com.squirrel.index12306.biz.ticketservice.service;

import com.squirrel.index12306.biz.ticketservice.dto.req.RegionStationQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.RegionStationQueryRespDTO;

import java.util.List;

/**
 * 地区以及车站接口层
 */
public interface RegionStationService {

    /**
     * 查询车站&城市站点集合信息
     *
     * @param requestParam 车站&城市查询参数
     * @return 车站&城市站点集合信息
     */
    List<RegionStationQueryRespDTO> listRegionStationQuery(RegionStationQueryReqDTO requestParam);
}
