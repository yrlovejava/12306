package com.squirrel.index12306.biz.ticketservice.mq.consumer;

import cn.hutool.core.collection.CollUtil;
import com.squirrel.index12306.biz.ticketservice.common.constant.TicketRocketMQConstant;
import com.squirrel.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 列车车票余量缓存更新消费端
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = TicketRocketMQConstant.CANAL_COMMON_SYNC_TOPIC_KEY,
        consumerGroup = TicketRocketMQConstant.CANAL_SYNC_COMMON_CG_KEY
)
public class TicketAvailabilityCacheUpdateConsumer implements RocketMQListener<CanalBinlogEvent> {

    private final DistributedCache distributedCache;

    @Override
    public void onMessage(CanalBinlogEvent message) {
        // 1.参数校验
        if(message.getIsDdl() || CollUtil.isEmpty(message.getOld()) || !Objects.equals("UPDATE",message.getType())){
            // todo UPDATE 写为常量
            return;
        }

        // 获取旧数据
        List<Map<String, Object>> actualOldDataList = message.getOld().stream()
                .filter(each -> each.get("seat_status") != null)
                .toList();
        // 获取变更数据
        List<Map<String, Object>> messageDataList = message.getData();

        Map<String,Map<Integer,Integer>> cacheChangeKeyMap = new HashMap<>();
        for(int i = 0;i < messageDataList.size();i++){
            Map<String, Object> each = messageDataList.get(i);
            Map<String, Object> actualOldData = actualOldDataList.get(i);
            String seatStatus = actualOldData.get("seat_status").toString();
            int incr = Objects.equals(seatStatus,"0") ? -1 : 1;
            String trainId = each.get("train_id").toString();
            // 获取redis中key
            String hashCacheKey = TRAIN_STATION_REMAINING_TICKET + trainId + "_" + each.get("start_station") + "_" + each.get("end_station");
            Map<Integer, Integer> seatTypeMap = cacheChangeKeyMap.get(hashCacheKey);
            if(CollUtil.isEmpty(seatTypeMap)){
                seatTypeMap = new HashMap<>();
            }
            int seatType = Integer.parseInt(each.get("seat_type").toString());
            seatTypeMap.compute(seatType, (k, num) -> num == null ? incr : num + incr);
            cacheChangeKeyMap.put(hashCacheKey,seatTypeMap);
        }
        // 更新缓存中的余票
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        cacheChangeKeyMap.forEach((cacheKey, cacheVal) -> cacheVal.forEach((seatType, num) -> instance.opsForHash().increment(cacheKey, String.valueOf(seatType), num)));
    }
}
