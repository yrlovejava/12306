package com.squirrel.index12306.biz.orderservice;

import org.junit.jupiter.api.Test;

public class OrderServiceApplicationTests {

    private static final String SQL = "CREATE TABLE `t_order_item_%d` (\n" +
            "  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'ID',\n" +
            "  `order_sn` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '订单号',\n" +
            "  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',\n" +
            "  `username` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',\n" +
            "  `train_id` bigint(20) DEFAULT NULL COMMENT '列车ID',\n" +
            "  `carriage_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '车厢号',\n" +
            "  `seat_type` int(3) DEFAULT NULL COMMENT '座位类型',\n" +
            "  `seat_number` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '座位号',\n" +
            "  `real_name` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '真实姓名',\n" +
            "  `id_type` int(3) DEFAULT NULL COMMENT '证件类型',\n" +
            "  `id_card` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '证件号',\n" +
            "  `ticket_type` int(3) DEFAULT NULL COMMENT '车票类型',\n" +
            "  `phone` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '手机号',\n" +
            "  `status` int(3) DEFAULT NULL COMMENT '订单状态',\n" +
            "  `amount` int(11) DEFAULT NULL COMMENT '订单金额',\n" +
            "  `create_time` datetime DEFAULT NULL COMMENT '创建时间',\n" +
            "  `update_time` datetime DEFAULT NULL COMMENT '修改时间',\n" +
            "  `del_flag` tinyint(1) DEFAULT NULL COMMENT '删除标识',\n" +
            "  PRIMARY KEY (`id`)\n" +
            ") ENGINE=InnoDB AUTO_INCREMENT=1679509741801537537 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';";

    @Test
    void contextLoads() {
        for (int i = 0; i < 15; i++) {
            System.out.printf((SQL) + "%n", i);
        }
    }

}
