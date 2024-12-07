package com.squirrel.index12306.framework.starter.distributedid.core;

/**
 * ID 生产器
 */
public interface IdGenerator {

    /**
     * 下一个 ID
     * @return 下一个ID
     */
    default long nextId() {
        return 0L;
    }

    /**
     * 下一个 ID 字符串
     * @return 下一个 ID 字符串
     */
    default String nextIdStr() {
        return "";
    }
}
