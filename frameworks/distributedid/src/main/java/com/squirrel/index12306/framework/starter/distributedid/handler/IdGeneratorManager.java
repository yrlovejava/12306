package com.squirrel.index12306.framework.starter.distributedid.handler;

import com.squirrel.index12306.framework.starter.distributedid.core.IdGenerator;
import com.squirrel.index12306.framework.starter.distributedid.core.serviceid.DefaultServiceIdGenerator;
import com.squirrel.index12306.framework.starter.distributedid.core.serviceid.ServiceIdGenerator;
import lombok.NonNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ID 生成器管理
 */
public final class IdGeneratorManager {

    /**
     * ID 生成器管理容器
     */
    private static Map<String, IdGenerator> MANAGER = new ConcurrentHashMap<>();

    /**
     * 注册默认 ID 生成器
     */
    static {
        MANAGER.put("default",new DefaultServiceIdGenerator());
    }

    /**
     * 注册 ID 生成器
     * @param resource ID 生成器名称
     * @param idGenerator ID 生成器实例
     */
    public static void registerIdGenerator(@NonNull String resource, @NonNull IdGenerator idGenerator) {
        IdGenerator actual = MANAGER.get(resource);
        if (actual != null) {
            // 如果已经注册过相同类型的，就不再注册
            return;
        }
        MANAGER.put(resource, idGenerator);
    }

    /**
     * 根据 resource 获取 ID 生成器
     * @param resource ID 生成器名称
     * @return ID 生成器
     */
    public static IdGenerator getIdGenerator(@NonNull String resource) {
        return MANAGER.get(resource);
    }

    /**
     * 获取默认 ID 生成器
     * @return ID 生成器
     */
    public static ServiceIdGenerator getDefaultServiceIdGenerator() {
        return (ServiceIdGenerator) MANAGER.get("default");
    }
}
