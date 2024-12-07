package com.squirrel.index12306.framework.starter.distributedid.toolkit;

import com.squirrel.index12306.framework.starter.distributedid.core.snowflake.Snowflake;
import com.squirrel.index12306.framework.starter.distributedid.core.snowflake.SnowflakeIdInfo;
import com.squirrel.index12306.framework.starter.distributedid.handler.IdGeneratorManager;

/**
 * 分布式雪花 ID 生成方法类
 */
public final class SnowflakeIdUtil {

    /**
     * 雪花算法对象
     */
    private static Snowflake SNOWFLAKE;

    /**
     * 初始化雪花算法
     * @param snowflake 雪花算法对象
     */
    public static void initSnowflake(Snowflake snowflake) {
        SnowflakeIdUtil.SNOWFLAKE = snowflake;
    }

    /**
     * 获取雪花算法对象实例
     * @return 雪花算法对象
     */
    public static Snowflake getInstance(){
        return SNOWFLAKE;
    }

    /**
     * 获取雪花算法下一个ID
     * @return 雪花算法ID
     */
    public static long nextId() {
        return SNOWFLAKE.nextId();
    }

    /**
     * 获取雪花算法下一个字符串类型 ID
     * @return 雪花算法ID字符串类型
     */
    public static String nextIdStr() {
        return Long.toString(nextId());
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     * @param snowflakeId 雪花算法ID字符串类型
     * @return 雪花算法各部分
     */
    public static SnowflakeIdInfo parseSnowflakeIdInfo(String snowflakeId) {
        return SNOWFLAKE.parseSnowflakeId(Long.parseLong(snowflakeId));
    }

    /**
     * 根据 serviceId 生成雪花算法 ID
     * @param serviceId 服务id
     * @return 雪花算法 ID
     */
    public static long nextIdByService(String serviceId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().nextId(Long.parseLong(serviceId));
    }

    /**
     * 根据 {@param serviceId} 生成字符串类型雪花算法 ID
     */
    public static String nextIdStrByService(String serviceId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().nextIdStr(Long.parseLong(serviceId));
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     */
    public static SnowflakeIdInfo parseSnowflakeServiceId(String snowflakeId) {
        return IdGeneratorManager.getDefaultServiceIdGenerator().parseSnowflakeId(Long.parseLong(snowflakeId));
    }
}
