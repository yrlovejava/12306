package com.squirrel.index12306.biz.orderservice.service.impl;

import com.squirrel.index12306.biz.orderservice.dao.entity.OrderDO;
import com.squirrel.index12306.biz.orderservice.dao.entity.OrderItemDO;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderMapper;
import com.squirrel.index12306.biz.orderservice.dto.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.TicketOrderItemCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.service.OrderService;
import com.squirrel.index12306.framework.starter.distributedid.toolkit.SnowflakeIdUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 订单接口实现类
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 使用雪花算法生成订单号
        String orderSn = SnowflakeIdUtil.nextIdStr();
        // 构建订单实体
        OrderDO orderDO = OrderDO.builder()
                .orderSn(orderSn)
                .orderTime(new Date())
                .departure(requestParam.getDeparture())
                .arrival(requestParam.getArrival())
                .source(requestParam.getSource())
                .status(0)
                .username(requestParam.getUsername())
                .build();
        // 插入数据库
        orderMapper.insert(orderDO);
        // 获取订单明细
        List<TicketOrderItemCreateReqDTO> ticketOrderItems =
                requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        ticketOrderItems.forEach(each -> {
            OrderItemDO orderItemDO = OrderItemDO.builder()
                    .orderSn(orderSn)
                    .trainId(requestParam.getTrainId())
                    .seatNumber(each.getSeatNumber())
                    .carriageNumber(each.getCarriageNumber())
                    .realName(each.getRealName())
                    .phone(each.getPhone())
                    .amount(each.getAmount())
                    .idCard(each.getIdCard())
                    .idType(each.getIdType())
                    .status(0)
                    .build();
            orderItemDOList.add(orderItemDO);
        });
        // TODO 批量插入数据库
        orderItemDOList.forEach(orderItemMapper::insert);
        // 返回订单号
        return orderSn;
    }
}
