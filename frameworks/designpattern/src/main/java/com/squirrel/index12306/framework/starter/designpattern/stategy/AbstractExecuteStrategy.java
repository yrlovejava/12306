package com.squirrel.index12306.framework.starter.designpattern.stategy;

/**
 * 抽象策略模式的接口
 * @param <REQUEST> 泛型
 * @param <RESPONSE> 泛型
 */
public interface AbstractExecuteStrategy<REQUEST, RESPONSE> {

    /**
     * 执行策略标识
     * @return 策略标识
     */
    default String mark(){
        return null;
    }

    /**
     * 执行策略范匹配标识
     */
    default String patternMatchMark() {
        return null;
    }

    /**
     * 执行策略
     * @param requestParam 执行策略入参
     */
    default void execute(REQUEST requestParam) {

    }

    /**
     * 执行策略，带返回值
     * @param requestParam 执行策略入参
     * @return 执行策略后返回值
     */
    default RESPONSE executeResp(REQUEST requestParam) {
        return null;
    }
}
