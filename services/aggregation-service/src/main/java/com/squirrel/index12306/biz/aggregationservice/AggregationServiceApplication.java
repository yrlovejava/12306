package com.squirrel.index12306.biz.aggregationservice;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 12306 聚合服务应用启动器
 */
@SpringBootApplication(scanBasePackages = {
        "com.squirrel.index12306.biz.userservice",
        "com.squirrel.index12306.biz.ticketservice",
        "com.squirrel.index12306.biz.orderservice",
        "com.squirrel.index12306.biz.payservice"
})
@MapperScan(value = {
        "com.squirrel.index12306.biz.userservice.dao.mapper",
        "com.squirrel.index12306.biz.ticketservice.dao.mapper",
        "com.squirrel.index12306.biz.orderservice.dao.mapper",
        "com.squirrel.index12306.biz.payservice.dao.mapper"
})
@EnableRetry
@EnableFeignClients("com.squirrel.index12306.biz.ticketservice.remote")
@EnableCrane4j(enumPackages = "com.squirrel.index12306.biz.orderservice.common.enums")
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }
}
