package com.squirrel.index12306.biz.ticketservice.toolkit.base;

import com.squirrel.index12306.framework.starter.cache.DistributedCache;

import java.util.HashMap;

/**
 * 高铁二等座座位验证
 */
public class TrainSecondCheckSeat implements TrainBitMapCheckSeat {

    /**
     * 高铁二等座座位是否存在验证
     *
     * @param key              缓存Key
     * @param convert          座位统计Map
     * @param distributedCache 分布式缓存接口
     * @return 判断座位是否存在 true 存在 false 不存在
     */
    @Override
    public boolean checkSeat(String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache) {
        return false;
    }
}
