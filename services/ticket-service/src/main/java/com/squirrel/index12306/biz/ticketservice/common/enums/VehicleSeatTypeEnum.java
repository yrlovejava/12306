package com.squirrel.index12306.biz.ticketservice.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 交通工具座位类型
 */
@Getter
@RequiredArgsConstructor
public enum VehicleSeatTypeEnum {

    /**
     * 商务座
     */
    BUSINESS_CLASS(0, "BUSINESS_CLASS","商务座"),

    /**
     * 一等座
     */
    FIRST_CLASS(1, "FIRST_CLASS","一等座"),

    /**
     * 二等座
     */
    SECOND_CLASS(2, "SECOND_CLASS","二等座"),

    /**
     * 动卧
     */
    SLEEPER_CLASS(3,"SLEEPER_CLASS","动卧"),

    /**
     * 一等卧
     */
    FIRST_SLEEPER_CLASS(4,"FIRST_SLEEPER_CLASS","一等卧"),

    /**
     * 二等卧
     */
    SECOND_SLEEPER_CLASS(5,"SECOND_SLEEPER_CLASS","二等卧"),

    /**
     * 软卧
     */
    SOFT_SLEEPER_CLASS(6,"SOFT_SLEEPER_CLASS","软卧"),

    /**
     * 高级软卧
     */
    DELUXE_SOFT_SLEEPER_CLASS(7,"DELUXE_SOFT_SLEEPER_CLASS","高级软卧"),

    /**
     * 硬卧
     */
    HARD_SLEEPER_CLASS(8,"HARD_SLEEPER_CLASS","硬卧"),

    /**
     * 硬座
     */
    HARD_SEAT_CLASS(9,"HARD_SEAT_CLASS","硬座");

    private final Integer code;

    private final String name;

    private final String value;
    /**
     * 根据编码查找名称
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(VehicleSeatTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleSeatTypeEnum::getName)
                .orElse(null);
    }
}
