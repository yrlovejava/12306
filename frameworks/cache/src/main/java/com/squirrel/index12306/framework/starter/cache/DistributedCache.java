package com.squirrel.index12306.framework.starter.cache;

import com.squirrel.index12306.framework.starter.cache.core.CacheGetFilter;
import com.squirrel.index12306.framework.starter.cache.core.CacheGetIfAbsent;
import com.squirrel.index12306.framework.starter.cache.core.CacheLoader;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.redisson.api.RBloomFilter;

import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存
 */
public interface DistributedCache extends Cache{

    /**
     * 获取缓存，如果查询结果为空，调用 {@link CacheLoader} 加载缓存
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @return value
     * @param <T> 泛型
     */
    <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout);

    /**
     * 获取缓存，如果查询结果为空，调用 {@link CacheLoader} 加载缓存
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timout 超时时间
     * @param timeUnit 时间单位
     * @return value
     * @param <T> 泛型
     */
    <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timout, TimeUnit timeUnit);

    /**
     * 以一种“安全”的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的，缓存击穿、缓存雪崩场景，适用于不被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @return value
     * @param <T> 泛型
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader,long timeout);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存击穿、缓存雪崩场景，适用于不被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param bloomFilter 布隆过期器
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param bloomFilter 布隆过滤器
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，并通过 {@link CacheGetFilter} 解决布隆过滤器无法删除问题，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param bloomFilter 布隆过滤器
     * @param cacheCheckFilter 缓存过滤器，解决布隆过滤器无法删除的过滤器
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，并通过 {@link CacheGetFilter} 解决布隆过滤器无法删除问题，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param bloomFilter 布隆过滤器
     * @param cacheCheckFilter 缓存过滤器，解决布隆过滤器无法删除的过滤器
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，并通过 {@link CacheGetFilter} 解决布隆过滤器无法删除问题，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param bloomFilter 布隆过滤器
     * @param cacheCheckFilter 解决布隆过滤器无法删除的过滤器
     * @param cacheGetIfAbsent 解决缓存不存在的问题
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout,
                  RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent);

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，并通过 {@link CacheGetFilter} 解决布隆过滤器无法删除问题，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param bloomFilter 布隆过滤器
     * @param cacheCheckFilter 解决布隆过滤器无法删除的过滤器
     * @param cacheGetIfAbsent 解决缓存不存在的问题
     */
    <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit,
                  RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent);

    /**
     * 放入缓存，自定义超时时间
     * @param key key
     * @param value value
     * @param timeout 超时时间
     */
    void put(@NotBlank String key, Object value, long timeout);

    /**
     * 放入缓存，自定义超时时间
     * @param key key
     * @param value value
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     */
    void put(@NotBlank String key,Object value,long timeout,TimeUnit timeUnit);

    /**
     * 放入缓存，自定义超时时间
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     * @param key key
     * @param value value
     * @param timeout 超时时间
     * @param bloomFilter 布隆过滤器
     */
    void safePut(@NotBlank String key, Object value, long timeout,RBloomFilter<String> bloomFilter);

    /**
     * 放入缓存，自定义超时时间，并通过 key 加入布隆过滤器，极大概率通过此方式防止，缓存击穿、缓存击穿、缓存雪崩
     * @param key key
     * @param value value
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param bloomFilter 布隆过滤器
     */
    void safePut(@NotBlank String key,Object value,long timeout,TimeUnit timeUnit, RBloomFilter<String> bloomFilter);

    /**
     * 统计指定 key 存在数量
     * @param key 可变参数key
     * @return 存在数量
     */
    Long countExistingKeys(@NotNull String... key);
}