package com.squirrel.index12306.biz.ticketservice.toolkit.base;

import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高铁一等座座位验证
 */
public class TrainFirstCheckSeat implements TrainBitMapCheckSeat {

    /**
     * 高铁一等座座位是否存在验证
     *
     * @param key              缓存Key
     * @param convert          座位统计Map
     * @param distributedCache 分布式缓存接口
     * @return 判断座位是否存在 true 存在 false 不存在
     */
    @Override
    public boolean checkSeat(String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache) {
        boolean flag = false;
        ValueOperations<String, String> opsForValue = ((StringRedisTemplate) distributedCache.getInstance()).opsForValue();
        AtomicInteger matchCount = new AtomicInteger(0);
        for (int i = 0; i < 4; i++) {
            int cnt = 0;
            if (convert.containsKey(i)) {
                for (int j = 0; j < 7; j++) {
                    Boolean bit = opsForValue.getBit(key, i + j * 4);
                    if (null != bit && bit) {
                        cnt = cnt + 1;
                    }
                    if (cnt == convert.get(i)) {
                        matchCount.getAndIncrement();
                        break;
                    }
                }
                if (cnt != convert.get(i)) {
                    break;
                }
            }
            if (matchCount.get() == convert.size()) {
                flag = true;
                break;
            }
        }
        return flag;
    }
}
