package com.squirrel.index12306.biz.ticketservice.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 列车标签枚举
 */
@Getter
@RequiredArgsConstructor
public enum TrainTagEnum {

    FU_XING("0", "复兴号"),

    HIGH_SPEED_TRAIN("1", "GC-高铁/城际");

    private final String code;

    private final String name;

    /**
     * 根据编码查找名称
     */
    public static String findNameByCode(String code) {
        return Arrays.stream(TrainTagEnum.values())
                .filter(each -> Objects.equals(each.getCode(), code))
                .findFirst()
                .map(TrainTagEnum::getName)
                .orElse(null);
    }

    /**
     * 根据编码查找名称
     */
    public static List<String> findNameByCode(List<String> codes) {
        List<String> resultNames = new ArrayList<>();
        for (String code : codes) {
            String name = Arrays.stream(TrainTagEnum.values())
                    .filter(each -> Objects.equals(each.getCode(), code))
                    .findFirst()
                    .map(TrainTagEnum::getName)
                    .orElse(null);
            if (StrUtil.isNotBlank(name)) {
                resultNames.add(name);
            }
        }
        return resultNames;
    }
}
