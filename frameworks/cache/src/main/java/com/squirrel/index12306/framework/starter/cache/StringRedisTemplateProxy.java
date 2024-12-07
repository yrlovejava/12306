package com.squirrel.index12306.framework.starter.cache;

import com.alibaba.fastjson2.JSON;
import com.google.common.collect.Lists;
import com.squirrel.index12306.framework.starter.bases.Singleton;
import com.squirrel.index12306.framework.starter.cache.config.RedisDistributedProperties;
import com.squirrel.index12306.framework.starter.cache.core.CacheGetFilter;
import com.squirrel.index12306.framework.starter.cache.core.CacheGetIfAbsent;
import com.squirrel.index12306.framework.starter.cache.core.CacheLoader;
import com.squirrel.index12306.framework.starter.cache.toolkit.CacheUtil;
import com.squirrel.index12306.framework.starter.cache.toolkit.FastJson2Util;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 分布式缓存之操作 Redis 模板代理
 * 底层通过 {@link RedissonClient}、{@link StringRedisTemplate} 完成外观接口行为
 */
@RequiredArgsConstructor
public class StringRedisTemplateProxy implements DistributedCache{

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisDistributedProperties redisDistributedProperties;
    private final RedissonClient redissonClient;

    private static final String LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH = "lua/putIfAllAbsent.lua";
    private static final String SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX = "safe_get_distributed_lock_get:";

    /**
     * 获取缓存
     * @param key key
     * @param clazz 类型
     * @return 缓存中的value
     * @param <T> 泛型
     */
    @Override
    public <T> T get(String key, Class<T> clazz) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (String.class.isAssignableFrom(clazz)) {
            return (T) value;
        }
        return JSON.parseObject(value, FastJson2Util.buildType(clazz));
    }

    /**
     * 放入缓存
     * @param key key
     * @param value value
     */
    @Override
    public void put(String key, Object value) {
        this.put(key,value, redisDistributedProperties.getValueTimeout());
    }

    /**
     * 如果 keys 全部不存在，则新增，返回 ture，反之返回 false
     * @param keys key集合
     * @return 是否全部不存在
     */
    @Override
    public Boolean putIfAllAbsent(@NotNull Collection<String> keys) {
        DefaultRedisScript<Boolean> actual = Singleton.get(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH, () -> {
            DefaultRedisScript<Boolean> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_PUT_IF_ALL_ABSENT_SCRIPT_PATH)));
            redisScript.setResultType(Boolean.class);
            return redisScript;
        });
        Boolean result = stringRedisTemplate.execute(actual, Lists.newArrayList(keys), redisDistributedProperties.getValueTimeout().toString());
        return result != null && result;
    }

    /**
     * 删除缓存
     * @param key key
     * @return 是否删除成功
     */
    @Override
    public Boolean delete(String key) {
        return stringRedisTemplate.delete(key);
    }

    /**
     * 删除 keys,返回删除数量
     * @param keys key集合
     * @return 删除的数量
     */
    @Override
    public Long delete(Collection<String> keys) {
        return stringRedisTemplate.delete(keys);
    }

    /**
     * 查询是否存在key
     * @param key key
     * @return 是否存在
     */
    @Override
    public Boolean hasKey(String key) {
        return stringRedisTemplate.hasKey(key);
    }

    /**
     * 获取缓存组件实例
     * @return 缓存组件实例
     */
    @Override
    public Object getInstance() {
        return stringRedisTemplate;
    }

    /**
     * 获取缓存，如果查询结果为空，调用 {@link CacheLoader} 加载缓存
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @return value
     * @param <T> 泛型
     */
    @Override
    public <T> T get(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return this.get(key, clazz, cacheLoader, timeout,redisDistributedProperties.getValueTimeUnit());
    }

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
    @Override
    public <T> T get(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timout, TimeUnit timeUnit) {
        T result = this.get(key, clazz);
        if (!CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        return this.loadAndSet(key,cacheLoader,timout,redisDistributedProperties.getValueTimeUnit(),false,null);
    }

    /**
     * 加载缓存并在缓存中设置key-value
     * @param key key
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param safeFlag 是否安全加载
     * @param bloomFilter 布隆过滤器
     * @return value
     * @param <T> 泛型
     */
    private <T> T loadAndSet(String key,CacheLoader<T> cacheLoader,long timeout,TimeUnit timeUnit,boolean safeFlag,RBloomFilter<String> bloomFilter) {
        T result = cacheLoader.load();
        if (CacheUtil.isNullOrBlank(result)) {
            return result;
        }
        if (safeFlag) {
            // 安全方式设置key-value
            this.safePut(key,result,timeout,timeUnit,bloomFilter);
        }else {
            this.put(key,result,timeout,timeUnit);
        }
        return result;
    }

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
    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout) {
        return this.safeGet(key,clazz,cacheLoader,timeout,redisDistributedProperties.getValueTimeUnit());
    }

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存击穿、缓存雪崩场景，适用于不被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     */
    @Override
    public <T> T safeGet(@NotBlank String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit) {
        return this.safeGet(key,clazz,cacheLoader,timeout,timeUnit,null);
    }

    /**
     * 以一种"安全"的方式获取缓存，如查询结果为空，调用 {@link CacheLoader} 加载缓存
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     * @param key key
     * @param clazz 类型
     * @param cacheLoader 缓存加载器
     * @param timeout 超时时间
     * @param bloomFilter 布隆过期器
     */
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter) {
        return this.safeGet(key,clazz,cacheLoader,timeout,redisDistributedProperties.getValueTimeUnit(),bloomFilter,null,null);
    }

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
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        return this.safeGet(key,clazz,cacheLoader,timeout,timeUnit,bloomFilter,null,null);
    }

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
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return this.safeGet(key,clazz,cacheLoader,timeout,redisDistributedProperties.getValueTimeUnit(),bloomFilter,cacheCheckFilter,null);
    }

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
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter) {
        return this.safeGet(key,clazz,cacheLoader,timeout,timeUnit,bloomFilter,cacheCheckFilter,null);
    }

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
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        return this.safeGet(key, clazz, cacheLoader, timeout,redisDistributedProperties.getValueTimeUnit(),bloomFilter,cacheCheckFilter,cacheGetIfAbsent);
    }

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
    @Override
    public <T> T safeGet(String key, Class<T> clazz, CacheLoader<T> cacheLoader, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter, CacheGetFilter<String> cacheCheckFilter, CacheGetIfAbsent<String> cacheGetIfAbsent) {
        T result = this.get(key, clazz);
        // 缓存结果不等于空或空字符串直接返回，通过函数判断是否返回空，为了适配布隆过滤器无法删除的场景，两者都不成立，判断布隆过滤器是否存在，不存在返回空
        if (!CacheUtil.isNullOrBlank(result)
                || Optional.ofNullable(cacheCheckFilter).map(each -> each.filter(key)).orElse(false)
                || Optional.ofNullable(bloomFilter).map(each -> !each.contains(key)).orElse(false)
        ) {
            return result;
        }
        // 加锁保证线程安全
        RLock lock = redissonClient.getLock(SAFE_GET_DISTRIBUTED_LOCK_KEY_PREFIX);
        lock.lock();
        try {
            // 双重判定锁，减轻获得分布式锁后线程访问数据库压力
            if (CacheUtil.isNullOrBlank(result = get(key, clazz))) {
                // 如果访问 cacheLoader 加载数据为空，执行后置函数操作
                if (CacheUtil.isNullOrBlank(result = loadAndSet(key,cacheLoader,timeout,timeUnit,true,bloomFilter))) {
                    Optional.ofNullable(cacheGetIfAbsent).ifPresent(each -> each.execute(key));
                }
            }
        }finally {
            lock.unlock();
        }
        return result;
    }

    /**
     * 放入缓存，自定义超时时间
     * @param key key
     * @param value value
     * @param timeout 超时时间
     */
    @Override
    public void put(String key, Object value, long timeout) {
        this.put(key,value,timeout,redisDistributedProperties.getValueTimeUnit());
    }

    /**
     * 放入缓存，自定义超时时间
     * @param key key
     * @param value value
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     */
    @Override
    public void put(String key, Object value, long timeout, TimeUnit timeUnit) {
        String actual = value instanceof String ? (String) value : JSON.toJSONString(value);
        stringRedisTemplate.opsForValue().set(key,actual,timeout,timeUnit);
    }

    /**
     * 放入缓存，自定义超时时间
     * 通过此方式防止程序中可能出现的：缓存穿透、缓存击穿以及缓存雪崩场景，需要客户端传递布隆过滤器，适用于被外部直接调用的接口
     * @param key key
     * @param value value
     * @param timeout 超时时间
     * @param bloomFilter 布隆过滤器
     */
    @Override
    public void safePut(String key, Object value, long timeout, RBloomFilter<String> bloomFilter) {
        this.safePut(key,value,timeout,redisDistributedProperties.getValueTimeUnit(),bloomFilter);
    }

    /**
     * 放入缓存，自定义超时时间，并通过 key 加入布隆过滤器，极大概率通过此方式防止，缓存击穿、缓存击穿、缓存雪崩
     * @param key key
     * @param value value
     * @param timeout 超时时间
     * @param timeUnit 时间单位
     * @param bloomFilter 布隆过滤器
     */
    @Override
    public void safePut(String key, Object value, long timeout, TimeUnit timeUnit, RBloomFilter<String> bloomFilter) {
        put(key,value,timeout,timeUnit);
        bloomFilter.add(key);
    }

    /**
     * 统计指定 key 存在数量
     * @param keys 可变参数key
     * @return 存在数量
     */
    @Override
    public Long countExistingKeys(@NotNull String... keys) {
        return stringRedisTemplate.countExistingKeys(Lists.newArrayList(keys));
    }
}
