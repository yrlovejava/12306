package com.squirrel.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.squirrel.index12306.framework.starter.database.base.BaseDO;
import lombok.Data;

/**
 * 地区表实体
 */
@Data
@TableName("t_region")
public class RegionDO extends BaseDO {

    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 地区名称
     */
    @TableField("name")
    private String name;

    /**
     * 地区全名
     */
    @TableField("full_name")
    private String fullName;

    /**
     * 地区编码
     */
    @TableField("code")
    private String code;

    /**
     * 地区首字母
     */
    @TableField("initial")
    private String initial;

    /**
     * 拼音
     */
    @TableField("spell")
    private String spell;

    /**
     * 热门标识
     */
    @TableField("popular_flag")
    private Integer popularFlag;
}
