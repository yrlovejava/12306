package com.squirrel.index12306.framework.starter.cache.core;

/**
 * 缓存过滤
 * @param <T>
 */
@FunctionalInterface // 函数式接口
public interface CacheGetFilter<T> {

    /**
     * 缓存过滤
     * @param param 输出参数
     * @return {@code true} 如果输入参数匹配，否则 {@link Boolean#TRUE}
     */
    boolean filter(T param);
}
