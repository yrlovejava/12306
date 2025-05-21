package com.squirrel.index12306.biz.ticketservice.toolkit;

import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;

import java.util.ArrayList;
import java.util.List;

/**
 * 站点计算工具
 */
public final class StationCalculateUtil {

    /**
     * 计算出发站和终点站中间的路线（包含出发站和终点站）
     * @param stations 所有站点数据
     * @param startStation 出发站
     * @param endStation 终点站
     * @return 出发站和终点站中间的站点（包含出发站和终点站）
     */
    public static List<RouteDTO> throughStation(List<String> stations,String startStation,String endStation) {
        List<RouteDTO> routeToDeduct = new ArrayList<>();
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
        if(startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return routeToDeduct;
        }
        // A B C D -> A-B A-C A-D B-C B-D C-D
        for (int i = startIndex;i < endIndex;i++) {
            for(int j = i+1;j < endIndex;j++){
                String currentStation = stations.get(i);
                String nextStation = stations.get(j);
                RouteDTO routeDTO = new RouteDTO(currentStation,nextStation);
                routeToDeduct.add(routeDTO);
            }
        }
        return routeToDeduct;
    }

    /**
     * 计算出发站和终点站之间需要扣减余票的站点（包含出发站和终点站）
     * @param stations 所有站点数据
     * @param startStation 出发站
     * @param endStation 终点站
     * @return 出发站和终点站之间需要扣减余票的站点（包含出发站和终点站）
     */
    public static List<RouteDTO> takeoutStation(List<String> stations,String startStation,String endStation) {
        List<RouteDTO> takeoutStationList = new ArrayList<>();
        int startIndex = stations.indexOf(startStation);
        int endIndex = stations.indexOf(endStation);
        if(startIndex < 0 || endIndex < 0 || startIndex >= endIndex) {
            return takeoutStationList;
        }
        if(startIndex != 0) {
            // 如果出发站不是第一个站点，那么所有经过出发站的路线都需要扣减
            // 比如: A B C D 出发站为 B，那么 A-C A-D 都需要扣减
            for (int i = 0; i < startIndex; i++) {
                for(int j = 1;j < stations.size() - startIndex;j++){
                    String currentStation = stations.get(i);
                    String nextStation = stations.get(j + startIndex);
                    RouteDTO routeDTO = new RouteDTO(currentStation,nextStation);
                    takeoutStationList.add(routeDTO);
                }
            }
        }
        // 从出发站到终点站的路线肯定需要扣减
        for(int i = startIndex;i <= endIndex;i++) {
            for(int j = i+1;j < stations.size() && i < endIndex;j++){
                String currentStation = stations.get(i);
                String nextStation = stations.get(j);
                RouteDTO routeDTO = new RouteDTO(currentStation,nextStation);
                takeoutStationList.add(routeDTO);
            }
        }
        return takeoutStationList;
    }
}
