package com.squirrel.index12306.biz.gatewayservice.config;

import lombok.Data;

import java.util.List;

/**
 * 过滤器配置
 */
@Data
public class Config {

    /**
     * 黑名单路径
     */
    private List<String> blackPathPre;
}
