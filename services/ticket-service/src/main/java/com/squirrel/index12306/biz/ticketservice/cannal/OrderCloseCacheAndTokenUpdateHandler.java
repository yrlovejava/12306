package com.squirrel.index12306.biz.ticketservice.cannal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 订单表变更-订单关闭或取消后置处理组件
 */
@Component
@RequiredArgsConstructor
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent,Void> {

    private final DistributedCache distributedCache;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final TrainStationService trainStationService;

    @Override
    public void execute(CanalBinlogEvent message) {
        // 过滤掉非订单关闭或取消的事件
        List<Map<String,Object>> messageDataList = message.getData().stream()
                .filter(each -> each.get("status") != null)
                .filter(each -> Objects.equals(each.get("status"),"30"))
                .toList();
        if(CollUtil.isEmpty(messageDataList)){
            return;
        }
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // TODO Lua 脚本执行该流程
        for (Map<String, Object> each : messageDataList) {
            // 查询订单详情
            Result<TicketOrderDetailRespDTO> orderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(each.get("order_sn").toString());
            TicketOrderDetailRespDTO orderDetailResultData = orderDetailResult.getData();
            if(orderDetailResult.isSuccess() && orderDetailResultData != null){
                // 乘车人信息
                List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailResultData.getPassengerDetails();
                // 令牌桶的key
                Long trainId = orderDetailResultData.getTrainId();
                String ticketAvailabilityTokenBucketKey = TICKET_AVAILABILITY_TOKEN_BUCKET + trainId;
                // 遍历
                for (TicketOrderPassengerDetailRespDTO item : passengerDetails) {
                    // 获取所有路线
                    List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(
                            String.valueOf(trainId),
                            orderDetailResultData.getDeparture(),
                            orderDetailResultData.getArrival());
                    // 遍历路线
                    for (RouteDTO routeDTO : routeDTOList) {
                        // 获取缓存中路线缓存余票的key
                        String trainStationRemainingTicketKey = TRAIN_STATION_REMAINING_TICKET + StrUtil.join("_", trainId, routeDTO.getStartStation(), routeDTO.getEndStation());
                        // 缓存中增加余票数量
                        stringRedisTemplate.opsForHash().increment(trainStationRemainingTicketKey,String.valueOf(item.getSeatType()),1);
                        // 令牌桶增加令牌
                        String tokenBucketKey = StrUtil.join("_", routeDTO.getStartStation(), routeDTO.getEndStation(),item.getSeatType());
                        stringRedisTemplate.opsForHash().increment(ticketAvailabilityTokenBucketKey,tokenBucketKey,1);
                    }
                }
            }
        }
    }

    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getActualTable();
    }

    @Override
    public String patternMatchMark() {
        return CanalExecuteStrategyMarkEnum.T_ORDER.getPatternMatchTable();
    }
}
