package com.squirrel.index12306.framework.starter.cache.toolkit;

import com.google.common.base.Strings;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * 缓存工具类
 */
public final class CacheUtil {

    private static final String SPLICING_OPERATOR = "_";

    /**
     * 构建缓存标识
     *
     * @param keys 可变参数key
     * @return 缓存标识
     */
    public static String buildKey(String... keys) {
        Stream.of(keys).forEach(key -> {
            Optional.ofNullable(Strings.emptyToNull(key)).orElseThrow(() -> new RuntimeException("构建缓存 key 不允许为空"));
        });
        return String.join(SPLICING_OPERATOR, keys);
    }

    /**
     * 判断结果是否为空或空的字符串
     *
     * @param cacheVal value
     * @return 是否为空或空的字符串
     */
    public static boolean isNullOrBlank(Object cacheVal) {
        return cacheVal == null || (cacheVal instanceof String && Strings.isNullOrEmpty((String) cacheVal));
    }
}
