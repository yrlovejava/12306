package com.squirrel.index12306.biz.ticketservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 座位实体持久层
 */
public interface SeatMapper extends BaseMapper<SeatDO> {

    /**
     * 获取列车车厢余票集合
     * @param seatDO 座位实体
     * @param trainCarriageList 车厢号集合
     * @return 余票集合
     */
    List<Integer> listSeatRemainingTicket(@Param("seatDO") SeatDO seatDO,@Param("trainCarriageList") List<String> trainCarriageList);

    /**
     * 获取列车 startStation 到 endStation 区间可用座位集合
     * @param trainId 列车id
     * @param startStation 开始站
     * @param endStation 到达站
     * @param seatTypes 座位类型
     * @return 座位类型和座位数量
     */
    List<SeatTypeCountDTO> listSeatTypeCount(@Param("trainId") Long trainId, @Param("startStation") String startStation, @Param("endStation") String endStation, @Param("seatTypes")  List<Integer> seatTypes);
}
