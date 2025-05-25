package com.squirrel.index12306.biz.orderservice;

import cn.crane4j.spring.boot.annotation.EnableCrane4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 订单服务应用启动器
 */
@SpringBootApplication
@MapperScan("com.squirrel.index12306.biz.orderservice.dao.mapper")
@EnableCrane4j(enumPackages = "com.squirrel.index12306.biz.orderservice.common.enums")
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class,args);
    }
}
