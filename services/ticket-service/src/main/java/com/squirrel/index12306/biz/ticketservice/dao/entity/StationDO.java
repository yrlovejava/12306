package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 车站实体
 */
@Data
@TableName("t_station")
public class StationDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 车站编号
     */
    @TableField("code")
    private String code;

    /**
     * 车站名称
     */
    @TableField("name")
    private String name;

    /**
     * 拼音
     */
    @TableField("spell")
    private String spell;

    /**
     * 车站地区
     */
    @TableField("region")
    private String region;

    /**
     * 车站地区名称
     */
    @TableField("regin_name")
    private String region_name;
}
