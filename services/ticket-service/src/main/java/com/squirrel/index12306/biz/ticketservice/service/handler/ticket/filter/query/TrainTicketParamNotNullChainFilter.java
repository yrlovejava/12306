package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.filter.query;

import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.ticketservice.dto.req.TicketPageQueryReqDTO;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

/**
 * 购票流程过滤器之验证乘客是否重复购买
 */
@Component
public class TrainTicketParamNotNullChainFilter implements TrainTicketQueryChainFilter<TicketPageQueryReqDTO> {

    @Override
    public void handler(TicketPageQueryReqDTO requestParam) {
        if (StrUtil.isBlank(requestParam.getFromStation())){
            throw new ClientException("出发地不能为空");
        }
        if (StrUtil.isBlank(requestParam.getToStation())) {
            throw new ClientException("目的地不能为空");
        }
        if (requestParam.getDepartureDate() == null){
            throw new ClientException("出发日期不能为空");
        }
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
