package com.squirrel.index12306.framework.starter.cache;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Collection;

/**
 * 缓存接口
 */
public interface Cache {

    /**
     * 获取缓存
     * @param key key
     * @param clazz 类型
     * @return 缓存中的value
     * @param <T> 泛型
     */
    <T> T get(@NotBlank String key, Class<T> clazz);

    /**
     * 放入缓存
     * @param key key
     * @param value value
     */
    void put(@NotBlank String key,Object value);

    /**
     * 如果 keys 全部不存在，则新增，返回 ture，反之返回 false
     * @param keys key集合
     * @return 是否全部不存在
     */
    Boolean putIfAllAbsent(@NotNull Collection<String> keys);

    /**
     * 删除缓存
     * @param key key
     * @return 是否删除成功
     */
    Boolean delete(@NotBlank String key);

    /**
     * 删除 keys,返回删除数量
     * @param keys key集合
     * @return 删除的数量
     */
    Long delete(@NotNull Collection<String> keys);

    /**
     * 查询是否存在key
     * @param key key
     * @return 是否存在
     */
    Boolean hasKey(@NotBlank String key);

    /**
     * 获取缓存组件实例
     * @return 缓存组件实例
     */
    Object getInstance();
}
