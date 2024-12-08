package com.squirrel.index12306.biz.ticketservice.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPageQueryRespDTO;

/**
 * 车票接口
 */
public interface TicketService {

    /**
     * 根据条件分页查询车票
     *
     * @param requestParam 分页查询车票请求参数
     * @return 查询车票返回结果
     */
    IPage<TicketPageQueryRespDTO> pageListTicketQuery(TicketPageQueryReqDTO requestParam);
}
