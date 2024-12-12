package com.squirrel.index12306.biz.aggregationservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 12306 聚合服务应用启动器
 */
@SpringBootApplication(scanBasePackages = {
        "com.squirrel.index12306.biz.userservice",
        "com.squirrel.index12306.biz.ticketservice",
        "com.squirrel.index12306.biz.orderservice"
})
@MapperScan(value = {
        "com.squirrel.index12306.biz.userservice.dao.mapper",
        "com.squirrel.index12306.biz.ticketservice.dao.mapper",
        "com.squirrel.index12306.biz.orderservice.dao.mapper"
})
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }
}
