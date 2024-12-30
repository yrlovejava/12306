package com.squirrel.index12306.biz.userservice.common.enums;

import com.squirrel.index12306.framework.starter.convention.errorcode.IErrorCode;
import lombok.AllArgsConstructor;

/**
 * 用户注册错误码枚举
 */
@AllArgsConstructor
public enum UserRegisterErrorCodeEnum implements IErrorCode {

    USER_REGISTER_FAIL("A006000", "用户注册失败"),

    USER_NAME_NOTNULL("A006001", "用户名不能为空"),

    PASSWORD_NOTNULL("A006002", "密码不能为空"),

    PHONE_NOTNULL("A006003", "手机号不能为空"),

    ID_TYPE_NOTNULL("A006004", "证件类型不能为空"),

    ID_CARD_NOTNULL("A006005", "证件号不能为空"),

    HAS_USERNAME_NOTNULL("A006006", "用户名已存在");

    /**
     * 错误码
     */
    private final String code;

    /**
     * 错误提示消息
     */
    private final String message;

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
