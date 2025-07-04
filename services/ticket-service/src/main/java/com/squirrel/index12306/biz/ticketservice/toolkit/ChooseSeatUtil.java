package com.squirrel.index12306.biz.ticketservice.toolkit;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 选择座位转换工具类
 */
public final class ChooseSeatUtil {

    public static final String TRAIN_BUSINESS = "TRAIN_BUSINESS";
    public static final String TRAIN_FIRST = "TRAIN_FIRST";
    public static final String TRAIN_SECOND = "TRAIN_SECOND";

    interface StrPool {
        String A = "A";
        String B = "B";
        String C = "C";
        String D = "D";
        String F = "F";
    }

    /**
     * 选座座位分类 convert
     * @param mark 座位类别标识
     * @param chooseSeatList 选座座位集合
     * @return 选择座位位置 Map
     */
    public static HashMap<Integer,Integer> convert(String mark, List<String> chooseSeatList) {
        // 1.将选择的座位按照座位类别进行分类
        HashMap<Integer,Integer> actualChooseSeatMap = new HashMap<>(8);
        Map<String, List<String>> chooseSeatMap = chooseSeatList.stream()
                .collect(Collectors.groupingBy(seat -> seat.substring(0, 1)));

        // 2.根据座位类别进行转换
        chooseSeatMap.forEach((key,value) -> {
            switch (mark) {
                case TRAIN_BUSINESS -> {
                    switch (key) {
                        case StrPool.A -> actualChooseSeatMap.put(0,value.size());
                        case StrPool.C -> actualChooseSeatMap.put(1,value.size());
                        case StrPool.F -> actualChooseSeatMap.put(2,value.size());
                    }
                }
                case TRAIN_FIRST -> {
                    switch (key) {
                        case StrPool.A -> actualChooseSeatMap.put(0,value.size());
                        case StrPool.C -> actualChooseSeatMap.put(1,value.size());
                        case StrPool.D -> actualChooseSeatMap.put(2,value.size());
                        case StrPool.F -> actualChooseSeatMap.put(3,value.size());
                    }
                }
                case TRAIN_SECOND -> {
                    switch (key) {
                        case StrPool.A -> actualChooseSeatMap.put(0,value.size());
                        case StrPool.B -> actualChooseSeatMap.put(1,value.size());
                        case StrPool.C -> actualChooseSeatMap.put(2,value.size());
                        case StrPool.D -> actualChooseSeatMap.put(3,value.size());
                        case StrPool.F -> actualChooseSeatMap.put(4,value.size());
                    }
                }
            }
        });

        // 3.返回选择座位位置 Map
        return actualChooseSeatMap;
    }
}
