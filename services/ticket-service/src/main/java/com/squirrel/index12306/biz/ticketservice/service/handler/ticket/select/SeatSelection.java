package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.select;

import cn.hutool.core.collection.CollUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 座位选择器
 */
public class SeatSelection {

    /**
     * 选择邻座空闲的座位
     * @param numSeats 需要选择的座位数量
     * @param seatLayout 座位布局
     * @return 空闲的座位
     */
    public static int[][] adjacent(int numSeats, int[][] seatLayout) {
        int numRows = seatLayout.length;
        int numCols = seatLayout[0].length;
        List<int[]> selectedSeats = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                // 如果座位布局中该位置等于0，证明没有被选择
                if (seatLayout[i][j] == 0) {
                    // 这一排能选择的连续(紧挨着)的座位数量
                    int consecutiveSeats = 0;
                    for (int k = j; k < numCols; k++) {
                        if (seatLayout[i][k] == 0) {
                            consecutiveSeats++;
                            // 如果能够选择的连续座位数量达到了需要选择的座位数量，则直接在结果中添加
                            if (consecutiveSeats == numSeats) {
                                for (int l = k - numSeats + 1; l <= k; l++) {
                                    selectedSeats.add(new int[]{i, l});
                                }
                                break;
                            }
                        } else {
                            consecutiveSeats = 0;
                        }
                    }
                    if (!selectedSeats.isEmpty()) {
                        break;
                    }
                }
            }
            if (!selectedSeats.isEmpty()) {
                break;
            }
        }
        if (CollUtil.isEmpty(selectedSeats)) {
            return null;
        }
        // 处理返回的座位格式，因为数组是从下标为0开始的，但是用户看到的应该是从1开始的
        return convertToActualSeat(selectedSeats);
    }

    /**
     * 选择同车厢不邻座的空闲座位
     * @param numSeats 需要的座位数量
     * @param seatLayout 座位布局
     * @return 空闲的座位
     */
    public static int[][] nonAdjacent(int numSeats,int[][] seatLayout) {
        int numRows = seatLayout.length;
        int numCols = seatLayout[0].length;
        List<int[]> selectedSeats = new ArrayList<>();
        for (int i = 0; i < numRows; i++) {
            for (int j = 0; j < numCols; j++) {
                if(seatLayout[i][j] == 0){
                    selectedSeats.add(new int[]{i,j});
                    if(selectedSeats.size() == numSeats){
                        break;
                    }
                }
            }
            if(selectedSeats.size() == numSeats){
                break;
            }
        }
        return convertToActualSeat(selectedSeats);
    }

    /**
     * 处理返回的座位格式
     * 因为数组是从下标为0开始的，但是用户看到的应该是从1开始的
     * @param selectedSeats 选择好的座位
     * @return 处理后的座位
     */
    private static int[][] convertToActualSeat(List<int[]> selectedSeats){
        int[][] actualSeat = new int[selectedSeats.size()][2];
        for (int i = 0; i < selectedSeats.size(); i++) {
            int[] seat = selectedSeats.get(i);
            int row = seat[0] + 1;
            int col = seat[1] + 1;
            actualSeat[i][0] = row;
            actualSeat[i][1] = col;
        }
        return actualSeat;
    }

    public static void main(String[] args) {
        int[][] seatLayout = {
                {1, 1, 1, 1},
                {1, 1, 1, 0},
                {1, 1, 1, 0},
                {0, 0, 0, 0}
        };
        int[][] select = adjacent(2, seatLayout);
        System.out.println("成功预订商务座相邻座位，座位位置为：");
        assert select != null;
        for (int[] ints : select) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }

        int[][] seatLayoutTwo = {
                {1, 0, 1, 1},
                {1, 1, 0, 0},
                {1, 1, 1, 0},
                {0, 0, 0, 0}
        };
        int[][] selectTwo = nonAdjacent(5, seatLayoutTwo);
        System.out.println("成功预订商务座不相邻座位，座位位置为：");
        for (int[] ints : selectTwo) {
            System.out.printf("第 %d 排，第 %d 列%n", ints[0], ints[1]);
        }
    }
}
