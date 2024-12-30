package com.squirrel.index12306.biz.orderservice.service.impl;

import cn.hutool.core.text.StrBuilder;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.orderservice.common.enums.OrderCanalErrorCodeEnum;
import com.squirrel.index12306.biz.orderservice.common.enums.OrderStatusEnum;
import com.squirrel.index12306.biz.orderservice.dao.entity.OrderDO;
import com.squirrel.index12306.biz.orderservice.dao.entity.OrderItemDO;
import com.squirrel.index12306.biz.orderservice.dao.entity.OrderItemPassengerDO;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderItemMapper;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderMapper;
import com.squirrel.index12306.biz.orderservice.dao.mapper.OrderPassengerRelationMapper;
import com.squirrel.index12306.biz.orderservice.dto.domain.OrderStatusReversalDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderItemCreateReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.req.TicketOrderPageQueryReqDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import com.squirrel.index12306.biz.orderservice.mq.event.PayResultCallbackOrderEvent;
import com.squirrel.index12306.biz.orderservice.service.OrderItemService;
import com.squirrel.index12306.biz.orderservice.service.OrderPassengerRelationService;
import com.squirrel.index12306.biz.orderservice.service.OrderService;
import com.squirrel.index12306.biz.orderservice.service.orderid.OrderIdGeneratorManager;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.page.PageResponse;
import com.squirrel.index12306.framework.starter.database.toolkit.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 订单接口实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final OrderItemService orderItemService;
    private final OrderPassengerRelationService orderPassengerRelationService;
    private final OrderPassengerRelationMapper orderPassengerRelationMapper;
    private final RedissonClient redissonClient;

    /**
     * 查询火车票订单详情
     *
     * @param orderSn 订单号
     * @return 订单详情
     */
    @Override
    public TicketOrderDetailRespDTO queryTicketOrderByOrderSn(String orderSn) {
        // 根据订单号查询数据库中订单信息
        OrderDO orderDO = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn));
        // 转换为订单详情返回参数
        TicketOrderDetailRespDTO result = BeanUtil.convert(orderDO, TicketOrderDetailRespDTO.class);
        // 查询数据库中订单明细
        List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(Wrappers.lambdaQuery(OrderItemDO.class)
                .eq(OrderItemDO::getOrderSn, orderSn));
        // 转换订单明细为乘车人详细返回信息
        result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
        // 返回结果
        return result;
    }

    /**
     * 根据用户id分页查询车票订单
     *
     * @param requestParam 分页查询参数
     * @return 订单详情
     */
    @Override
    public PageResponse<TicketOrderDetailRespDTO> pageTicketOrder(TicketOrderPageQueryReqDTO requestParam) {
        LambdaQueryWrapper<OrderDO> queryWrapper = Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getUserId, requestParam.getUserId())
                .orderByDesc(OrderDO::getOrderTime);
        // 根据用户id查询数据库中的订单信息
        IPage<OrderDO> orderPage = orderMapper.selectPage(PageUtil.convert(requestParam), queryWrapper);
        // 封装返回数据
        return PageUtil.convert(orderPage, each -> {
            // 转换为订单详情返回参数
            TicketOrderDetailRespDTO result = BeanUtil.convert(each, TicketOrderDetailRespDTO.class);
            // 查询数据库中订单明细
            LambdaQueryWrapper<OrderItemDO> orderItemQueryWrapper = Wrappers.lambdaQuery(OrderItemDO.class)
                    .eq(OrderItemDO::getUserId, requestParam.getUserId());
            List<OrderItemDO> orderItemDOList = orderItemMapper.selectList(orderItemQueryWrapper);
            // 转换订单明细为乘车人详细返回信息
            result.setPassengerDetails(BeanUtil.convert(orderItemDOList, TicketOrderPassengerDetailRespDTO.class));
            // 返回结果
            return result;
        });
    }

    /**
     * 创建火车票订单
     *
     * @param requestParam 商品订单入参
     * @return 订单号
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createTicketOrder(TicketOrderCreateReqDTO requestParam) {
        // 通过基因法将用户 ID 融入到订单号
        String orderSn = OrderIdGeneratorManager.generateId(requestParam.getUserId());
        // 构建订单实体
        OrderDO orderDO = OrderDO.builder()
                .orderSn(orderSn)
                .orderTime(requestParam.getOrderTime())
                .departure(requestParam.getDeparture())
                .arrival(requestParam.getArrival())
                .trainId(requestParam.getTrainId())
                .source(requestParam.getSource())
                .status(OrderStatusEnum.PENDING_PAYMENT.getStatus())
                .username(requestParam.getUsername())
                .userId(requestParam.getUserId())
                .build();
        // 插入数据库
        orderMapper.insert(orderDO);
        // 获取订单明细和订单和乘车人的关系
        List<TicketOrderItemCreateReqDTO> ticketOrderItems =
                requestParam.getTicketOrderItems();
        List<OrderItemDO> orderItemDOList = new ArrayList<>();
        List<OrderItemPassengerDO> orderItemPassengerDOList = new ArrayList<>();
        ticketOrderItems.forEach(each -> {
            // 订单明细
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
            // 订单和乘车人关系
            OrderItemPassengerDO orderItemPassengerDO = OrderItemPassengerDO.builder()
                    .idType(each.getIdType())
                    .idCard(each.getIdCard())
                    .orderSn(orderSn)
                    .build();
            orderItemPassengerDOList.add(orderItemPassengerDO);
        });
        // 批量插入数据库
        // 订单明细
        orderItemService.saveBatch(orderItemDOList);
        // 订单和乘车人的关系
        orderItemPassengerDOList.forEach(orderPassengerRelationMapper::insert);
        // 返回订单号
        return orderSn;
    }

    /**
     * 关闭火车票订单
     *
     * @param orderSn 订单号
     */
    @Override
    public void closeTickOrder(String orderSn) {
        // 在数据库中查询订单
        OrderDO orderDO = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn));
        // 判断订单状态，只有待付款状态才能关闭
        if (Objects.isNull(orderDO) || orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            return;
        }
        // 原则上订单关闭和订单取消这两个方法可以复用，为了区分未来考虑到的场景，这里对方法进行拆分但服用逻辑
        this.cancelTickOrder(orderSn);
    }

    /**
     * 取消火车票订单
     *
     * @param orderSn 订单号
     */
    @Override
    public void cancelTickOrder(String orderSn) {
        // 在数据库中查询订单
        OrderDO orderDO = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, orderSn));
        if(orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        }else if(orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()){
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        // 加分布式锁，保证并发安全
        // TODO 锁的key写为常量
        RLock lock = redissonClient.getLock(StrBuilder.create("order:canal:order_sn_").append(orderSn).toString());
        if(!lock.tryLock()) {
            throw new ClientException(OrderCanalErrorCodeEnum.ORDER_CANAL_REPETITION_ERROR);
        }
        try {
            // 修改数据库中订单的状态
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(OrderStatusEnum.CLOSED.getStatus());
            updateOrderDO.setOrderSn(orderSn);
            int updateResult = orderMapper.update(updateOrderDO, Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, orderSn));
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
            // 修改订单明细状态
            OrderItemDO updateOrderItemDO = new OrderItemDO();
            updateOrderItemDO.setStatus(OrderStatusEnum.CLOSED.getStatus());
            updateOrderItemDO.setOrderSn(orderSn);
            int updateItemResult = orderItemMapper.update(updateOrderItemDO, Wrappers.lambdaUpdate(OrderItemDO.class)
                    .eq(OrderItemDO::getOrderSn, orderSn));
            if (updateItemResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_ERROR);
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * 订单状态反转
     *
     * @param requestParam 请求参数
     */
    @Override
    public void statusReversal(OrderStatusReversalDTO requestParam) {
        // 在数据库中查询订单
        OrderDO orderDO = orderMapper.selectOne(Wrappers.lambdaQuery(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn()));
        if (orderDO == null) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_UNKNOWN_ERROR);
        } else if (orderDO.getStatus() != OrderStatusEnum.PENDING_PAYMENT.getStatus()) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_CANAL_STATUS_ERROR);
        }
        // 加分布式锁
        // TODO 锁前缀要写为常量
        RLock lock = redissonClient.getLock(StrBuilder.create("order:status-reversal:order_sn_").append(requestParam.getOrderSn()).toString());
        if (!lock.tryLock()) {
            log.warn("订单重复修改状态，状态反转请求参数：{}", JSON.toJSONString(requestParam));
        }
        try {
            OrderDO updateOrderDO = new OrderDO();
            updateOrderDO.setStatus(requestParam.getOrderStatus());
            // 在数据库中修改订单状态
            int updateResult = orderMapper.update(updateOrderDO, Wrappers.lambdaUpdate(OrderDO.class)
                    .eq(OrderDO::getOrderSn, requestParam.getOrderSn()));
            if (updateResult <= 0) {
                throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
            }
        }finally {
            lock.unlock();
        }
    }

    /**
     * 支付结果回调订单
     *
     * @param requestParam 请求参数
     */
    @Override
    public void payCallbackOrder(PayResultCallbackOrderEvent requestParam) {
        OrderDO updateOrderDO = new OrderDO();
        updateOrderDO.setPayTime(requestParam.getGmtPayment());
        updateOrderDO.setPayType(requestParam.getChannel());
        // 修改数据库中订单
        int updateResult = orderMapper.update(updateOrderDO, Wrappers.lambdaUpdate(OrderDO.class)
                .eq(OrderDO::getOrderSn, requestParam.getOrderSn()));
        if (updateResult <= 0) {
            throw new ServiceException(OrderCanalErrorCodeEnum.ORDER_STATUS_REVERSAL_ERROR);
        }
    }
}
