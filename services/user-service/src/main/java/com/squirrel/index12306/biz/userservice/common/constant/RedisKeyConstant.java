package com.squirrel.index12306.biz.userservice.common.constant;

/**
 * Redis Key 定义常量类
 */
public final class RedisKeyConstant {

    /**
     * 用户注销锁，Key Prefix + 用户名
     */
    public static final String USER_DELETION = "index12306-user-service:user-deletion:";
}
