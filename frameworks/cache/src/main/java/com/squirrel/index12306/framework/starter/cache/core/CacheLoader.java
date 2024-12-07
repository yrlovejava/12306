package com.squirrel.index12306.framework.starter.cache.core;

/**
 * 缓存加载器
 */
@FunctionalInterface
public interface CacheLoader<T> {

    /**
     * 加载缓存
     * @return T
     */
    T load();
}
