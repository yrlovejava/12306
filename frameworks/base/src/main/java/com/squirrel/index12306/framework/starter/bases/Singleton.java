package com.squirrel.index12306.framework.starter.bases;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * 单例对象容器
 */
public final class Singleton {

    private static final ConcurrentHashMap<String, Object> SINGLE_OBJECT_POOL = new ConcurrentHashMap<>();

    /**
     * 根据 key 获取单例对象
     *
     * @param key key
     * @param <T> 泛型
     * @return 单例对象
     */
    public static <T> T get(String key) {
        Object result = SINGLE_OBJECT_POOL.get(key);
        return result == null ? null : (T) result;
    }

    /**
     * 根据 key 获取单例对象
     * <p> 为空时，通过 supplier 构建单例对象并放入容器
     *
     * @param key      key
     * @param supplier 函数式接口，作用是返回指定类型的对象或值
     * @param <T>      泛型
     * @return 单例对象
     */
    public static <T> T get(String key, Supplier<T> supplier) {
        Object result = SINGLE_OBJECT_POOL.get(key);
        if (result == null && (result = supplier.get()) != null) {
            SINGLE_OBJECT_POOL.put(key, result);
        }
        return result == null ? null : (T) result;
    }

    /**
     * 对象放入容器，默认用对象的class名作为key
     * @param value 对象
     */
    public static void put(Object value) {
        put(value.getClass().getName(), value);
    }

    /**
     * 对象放入容器
     * @param key key
     * @param value 对象
     */
    public static void put(String key, Object value) {
        SINGLE_OBJECT_POOL.put(key, value);
    }
}
