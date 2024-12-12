package com.squirrel.index12306.biz.aggregationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 12306 聚合服务应用启动器
 */
@SpringBootApplication(scanBasePackageClasses = {
        "com.squirrel.index12306.biz.userservice",
        "com.squirrel.index12306.biz.ticketservice"
})
public class AggregationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AggregationServiceApplication.class, args);
    }
}
