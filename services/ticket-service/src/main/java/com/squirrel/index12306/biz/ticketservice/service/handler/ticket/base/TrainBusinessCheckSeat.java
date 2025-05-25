package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base;

import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 高铁商务座验证座位
 */
public class TrainBusinessCheckSeat implements TrainBitMapCheckSeat {

    /**
     * 高铁商务座是否存在检测方法
     *
     * @param key              缓存Key
     * @param convert          座位统计Map
     * @param distributedCache 分布式缓存接口
     * @return 判断座位是否存在 true or false
     */
    @Override
    public boolean checkSeat(final String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache) {
        boolean flag = false;
        ValueOperations<String, String> opsForValue = ((StringRedisTemplate) distributedCache.getInstance()).opsForValue();
        AtomicInteger matchCount = new AtomicInteger(0);
        for (int i = 0; i < 3; i++) {
            int cnt = 0;
            for (int j = 0; j < 2; j++) {
                Boolean bit = opsForValue.getBit(key, i + j * 3);
                if (null != bit && bit) {
                    cnt = cnt + 1;
                }
                if (cnt == convert.get(i)) {
                    matchCount.getAndIncrement();
                    break;
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
