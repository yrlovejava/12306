package com.squirrel.index12306.biz.ticketservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 购票服务应用启动器
 */
@SpringBootApplication
@MapperScan("com.squirrel.index12306.biz.ticketservice.dao.mapper")
public class TicketServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
