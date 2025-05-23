package com.squirrel.index12306.biz.ticketservice.toolkit;

import cn.hutool.core.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 座位统计工具类
 */
public final class CarriageVacantSeatCalculateUtil {

    /**
     * 座位统计方法
     *
     * @param actualSeats 座位选座情况二维数组
     * @param n 列数
     * @param m 行数
     * @return 座位统计结果
     */
    public static PriorityQueue<List<Pair<Integer,Integer>>> buildCarriageVacantSeatList(int[][] actualSeats,int n,int m){
        PriorityQueue<List<Pair<Integer, Integer>>> vacantSeatQueue  = new PriorityQueue<>((a, b) -> b.size() - a.size());

        for(int i = 0;i < n;i++){
            for(int j = 0;j < m;j++){
                if(actualSeats[i][j] == 1){
                    continue;
                }
                // 查找连续空余座位，seatList代表连续空余座位的集合
                List<Pair<Integer, Integer>> seatList = new ArrayList<>();
                int k = j;
                for(;k < 3;k++){
                    if(actualSeats[i][k] == 1){
                        break;
                    }
                    seatList.add(new Pair<>(i,k));
                }
                j = k;
                vacantSeatQueue.add(seatList);
            }
        }

        return vacantSeatQueue;
    }
}
