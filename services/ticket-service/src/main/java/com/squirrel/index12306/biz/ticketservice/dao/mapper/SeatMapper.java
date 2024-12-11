package com.squirrel.index12306.biz.ticketservice.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
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
}
