package com.squirrel.index12306.biz.ticketservice.toolkit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 日期工具类
 */
@Slf4j
public final class DateUtil {

    /**
     * 计算小时差
     *
     * @param startTime 开始时间 2022-10-01 00:00:00
     * @param endTime   结束时间 2022-10-01 12:23:00
     * @return 12:23
     */
    public static String calculateHourDifference(Date startTime, Date endTime) {
        // 1.将开始时间转换为带时区的时间
        LocalDateTime startDateTime = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        // 2.将结束时间转换为带时区的时间
        LocalDateTime endDateTime = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        // 3.获取时间间隔
        Duration duration = Duration.between(startDateTime, endDateTime);
        // 转换为小时
        long hours = duration.toHours();
        // 转换为分钟
        long minutes = duration.toMinutes() % 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    @SneakyThrows
    public static void main(String[] args) {
        String startTimeStr = "2022-10-01 01:00:00";
        String endTimeStr = "2022-10-01 12:23:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = formatter.parse(startTimeStr);
        Date endTime = formatter.parse(endTimeStr);
        String calculateHourDifference = calculateHourDifference(startTime, endTime);
        log.info("开始时间：{}，结束时间：{}，两个时间相差时分：{}", startTimeStr, endTimeStr, calculateHourDifference);
    }
}
