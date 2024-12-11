package com.squirrel.index12306.biz.ticketservice.common.constant.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Objects;

/**
 * 交通工具类型
 */
@Getter
@RequiredArgsConstructor
public enum VehicleTypeEnum {

    /**
     * 高铁
     */
    HIGH_SPEED_RAIN(0, "HIGH_SPEED_RAIN"),

    /**
     * 火车
     */
    TRAIN(1, "TRAIN"),

    /**
     * 汽车
     */
    CAR(2, "CAR"),

    /**
     * 飞机
     */
    AIRPLANE(3, "AIRPLANE");

    private final Integer code;

    private final String name;

    /**
     * 根据编码查找名称
     */
    public static String findNameByCode(Integer code) {
        return Arrays.stream(VehicleTypeEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(VehicleTypeEnum::getName)
                .orElse(null);
    }
}
