package com.squirrel.index12306.biz.userservice.toolkit;

import static com.squirrel.index12306.biz.userservice.common.constant.Index12306Constant.USER_REGISTER_REUSE_SHARDING_COUNT;

/**
 * 用户可复用工具类
 */
public final class UserReuseUtil {

    /**
     * 计算分片位置
     * @param username 用户名
     */
    public static int hashShardingIdx(String username) {
        return Math.abs(username.hashCode() % USER_REGISTER_REUSE_SHARDING_COUNT);
    }
}
