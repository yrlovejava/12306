package com.squirrel.index12306.biz.ticketservice.cannal;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.ticketservice.common.enums.CanalExecuteStrategyMarkEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.mq.event.CanalBinlogEvent;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 座位表变更-列车车票余量缓存更新组件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketAvailabilityCacheUpdateHandler implements AbstractExecuteStrategy<CanalBinlogEvent, Void> {

    private final DistributedCache distributedCache;

    @Override
    public void execute(CanalBinlogEvent message) {
        List<Map<String,Object>> messageDataList = new ArrayList<>();
        List<Map<String,Object>> actualOldDataList = new ArrayList<>();
        for (int i = 0; i < message.getOld().size(); i++) {
            Map<String, Object> oldDataMap = message.getOld().get(i);
            if(oldDataMap.get("seat_status") != null && StrUtil.isNotBlank(oldDataMap.get("seat_status").toString())){
                Map<String, Object> curDataMap = message.getData().get(i);
                if(StrUtil.equalsAny(
                        curDataMap.get("seat_status").toString(),
                        String.valueOf(SeatStatusEnum.AVAILABLE.getCode()),
                        String.valueOf(SeatStatusEnum.LOCKED.getCode())
                )){
                    actualOldDataList.add(oldDataMap);
                    messageDataList.add(curDataMap);
                }
            }
        }

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

    @Override
    public String mark() {
        return CanalExecuteStrategyMarkEnum.T_SEAT.getActualTable();
    }
}
