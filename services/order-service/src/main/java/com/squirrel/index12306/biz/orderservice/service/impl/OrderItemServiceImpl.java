package com.squirrel.index12306.biz.orderservice.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.squirrel.index12306.biz.orderservice.dao.entity.OrderItemDO;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import com.squirrel.index12306.biz.orderservice.service.OrderItemService;
import org.springframework.stereotype.Service;

/**
 * 订单明细接口层实现
 */
@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItemDO> implements OrderItemService {


}
