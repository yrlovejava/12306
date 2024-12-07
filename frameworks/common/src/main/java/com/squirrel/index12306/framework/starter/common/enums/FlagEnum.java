package com.squirrel.index12306.framework.starter.common.enums;

/**
 * 标识枚举，非 {@link Boolean#TRUE} 即 {@link Boolean#FALSE}
 */
public enum FlagEnum {

    /**
     * 正常状态
     */
    FALSE(0),

    /**
     * 删除状态
     */
    TRUE(1);

    private final Integer flag;

    FlagEnum(Integer flag) {
        this.flag = flag;
    }

    public Integer code() {
        return this.flag;
    }

    public String strCode() {
        return String.valueOf(this.flag);
    }

    @Override
    public String toString() {
        return strCode();
    }
}
