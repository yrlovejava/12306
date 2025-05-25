package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base;

import com.squirrel.index12306.framework.starter.cache.DistributedCache;

import java.util.HashMap;

/**
 * 抽象的验证座位接口
 */
public interface BitMapCheckSeat {
    /**
     * 座位是否存在检查方法
     *
     * @param key              缓存Key
     * @param convert          座位统计Map
     * @param distributedCache 分布式缓存接口
     * @return 判断座位是否存在 true or false
     */
    boolean checkSeat(String key, HashMap<Integer, Integer> convert, DistributedCache distributedCache);
}
