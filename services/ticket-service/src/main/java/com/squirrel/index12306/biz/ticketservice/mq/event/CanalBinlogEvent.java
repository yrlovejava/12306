package com.squirrel.index12306.biz.ticketservice.mq.event;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Canal Binlog 监听触发时间
 */
@Data
public class CanalBinlogEvent {

    /**
     * 变更数据
     */
    private List<Map<String,Object>> data;

    /**
     * 数据库名称
     */
    private String database;

    /**
     * 看着像时间戳
     */
    private long es;

    /**
     * 递增 ID，从 1 开始
     */
    private Long id;

    /**
     * 当前变更是否是 DDL 语句
     */
    private Boolean isDdl;

    /**
     * 表结构字段类型
     */
    private List<Map<String,Object>> mysqlType;

    /**
     * UPDATE 模式下的旧数据
     */
    private List<Map<String,Object>> old;

    /**
     * 主键名称
     */
    private List<String> pkNames;

    /**
     * SQL 语句
     */
    private String sql;

    /**
     * SQL 类型
     */
    private Map<String,Object> sqlType;

    /**
     * 表名
     */
    private String table;

    private Long ts;

    /**
     * 操作类型 INSERT（新增）、UPDATE（更新）、DELETE（删除）等等
     */
    private String type;
}
