package com.squirrel.index12306.framework.starter.convention.errorcode;

/**
 * 平台错误码
 */
public interface IErrorCode {

    /**
     * 错误码
     * @return 错误码
     */
    String code();

    /**
     * 错误信息
     * @return 错误信息
     */
    String message();
}
