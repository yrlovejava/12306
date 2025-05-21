package com.squirrel.index12306.biz.ticketservice.cannal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.squirrel.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.resp.TicketPurchaseRespDTO;
import com.squirrel.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.squirrel.index12306.biz.ticketservice.remote.TicketOrderRemoteService;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.bases.Singleton;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.Assert;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;

/**
 * 订单表变更-订单关闭或取消后置处理组件
 * 1.解锁锁定的座位
 * 2.增加令牌桶中令牌的数量
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCloseCacheAndTokenUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent,Void> {

    private final DistributedCache distributedCache;
    private final TicketOrderRemoteService ticketOrderRemoteService;
    private final TrainStationService trainStationService;
    private final SeatService seatService;

    private static final String LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH = "lua/ticket_availability_rollback_token_bucket.lua.lua";

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
        for (Map<String, Object> each : messageDataList) {
            // 查询订单详情
            Result<TicketOrderDetailRespDTO> orderDetailResult = ticketOrderRemoteService.queryTicketOrderByOrderSn(each.get("order_sn").toString());
            TicketOrderDetailRespDTO orderDetailResultData = orderDetailResult.getData();
            if(orderDetailResult.isSuccess() && orderDetailResultData != null){
                String trainId = String.valueOf(orderDetailResultData.getTrainId());
                // 乘车人信息
                List<TicketOrderPassengerDetailRespDTO> passengerDetails = orderDetailResultData.getPassengerDetails();
                // 解锁锁定的座位
                List<TrainPurchaseTicketRespDTO> purchaseRespDTOList = BeanUtil.convert(passengerDetails, TrainPurchaseTicketRespDTO.class);
                seatService.unlock(trainId,orderDetailResultData.getDeparture(),orderDetailResultData.getArrival(),purchaseRespDTOList);

                // 增加令牌桶中令牌的数量
                // 单例获取LUA执行脚本
                DefaultRedisScript<Long> actual = Singleton.get(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH,() -> {
                    DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
                    redisScript.setResultType(Long.class);
                    redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH)));
                    return redisScript;
                });
                Assert.notNull(actual);
                // 拼接LUA脚本的参数
                // 获取座位类型对应的数量
                Map<Integer,Long> seatTypeCountMap = passengerDetails.stream()
                        .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType,Collectors.counting()));
                JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                        .map(entry -> {
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("seatType", String.valueOf(entry.getKey()));
                            jsonObject.put("count", String.valueOf(entry.getValue()));
                            return jsonObject;
                        })
                        .collect(Collectors.toCollection(JSONArray::new));
                // 获取redisTemplate
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // 拼接令牌桶key
                String bucketKey = TICKET_AVAILABILITY_TOKEN_BUCKET + trainId;
                // 拼接hashKey
                String hashKey = StrUtil.join("_",orderDetailResultData.getDeparture(),orderDetailResultData.getArrival());
                // 计算需要扣减的路线（也就是现在需要回滚的路线）
                List<RouteDTO> routeDTOList = trainStationService.
                        listTakeoutTrainStationRoute(trainId, orderDetailResultData.getDeparture(), orderDetailResultData.getArrival());
                // 执行LUA脚本
                Long result = stringRedisTemplate.execute(
                        actual,
                        List.of(bucketKey, hashKey),
                        JSON.toJSONString(seatTypeCountArray),
                        JSON.toJSONString(routeDTOList)
                );

                // 判断结果
                if(result == null || !Objects.equals(result,0L)){
                    log.error("回滚列车余票令牌失败，订单信息: {}",JSON.toJSONString(orderDetailResultData));
                    throw new ServiceException("回滚列车余票失败");
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
