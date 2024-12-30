package com.squirrel.index12306.biz.orderservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.squirrel.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderPassengerRelationMapper;
import com.squirrel.index12306.biz.orderservice.service.OrderPassengerRelationService;
import org.springframework.stereotype.Service;

/**
 * 乘车人订单关系接口实现层
 */
@Service
public class OrderPassengerRelationServiceImpl extends ServiceImpl<OrderPassengerRelationMapper, OrderItemPassengerDO> implements OrderPassengerRelationService {
}
