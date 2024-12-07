package com.squirrel.index12306.framework.starter.cache.core;

/**
 * 缓存查询为空
 * @param <T>
 */
@FunctionalInterface
public interface CacheGetIfAbsent<T> {

    /**
     * 如果查询为空，执行逻辑
     * @param param 输入参数
     */
    void execute(T param);
}
