package com.squirrel.index12306.biz.ticketservice.toolkit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;

/**
 * 匹配剩余的座位工具类
 */
public final class SurplusNeedMatchSeatUtil {

    /**
     * 匹配指定数量的空余座位方法
     * @param chooseSeatSize 选择座位数量
     * @param vacantSeatQueue 空余座位集合
     * @return 获取选择座位数量的空余座位集合（获取数量可能小于选择座位数量）
     */
    public static List<Pair<Integer,Integer>> getSurplusNeedMatchSeat(int chooseSeatSize, PriorityQueue<List<Pair<Integer,Integer>>> vacantSeatQueue){
        // 1.使用并行流筛选出空余座位数量大于等于选择座位数量的集合
        Optional<List<Pair<Integer, Integer>>> optionalList = vacantSeatQueue.parallelStream()
                .filter(each -> each.size() >= chooseSeatSize)
                .findFirst();
        if(optionalList.isPresent()){
            // 2.如果存在，直接返回
            return optionalList.get().subList(0,chooseSeatSize);
        }

        List<Pair<Integer, Integer>> result = new ArrayList<>(chooseSeatSize);
        while (CollUtil.isNotEmpty(vacantSeatQueue)) {
            // 3.如果不存在，从剩余座位集合中依次取出座位，直到满足选择座位数量
            List<Pair<Integer, Integer>> seatList = vacantSeatQueue.poll();
            if(result.size() + seatList.size() < chooseSeatSize){
                // 3.1 如果当前座位数量加上剩余座位数量小于选择座位数量，直接添加
                result.addAll(seatList);
            }else if (result.size() + seatList.size() >= chooseSeatSize){
                // 3.2 如果当前座位数量加上剩余座位数量大于等于选择座位数量，取出需要的座位数量
                int needSize = chooseSeatSize - result.size();
                result.addAll(seatList.subList(0,needSize));
                if(result.size() == chooseSeatSize){
                    break;
                }
            }
        }
        return result;
    }
}
