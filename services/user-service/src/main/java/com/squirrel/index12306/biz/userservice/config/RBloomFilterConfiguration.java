package com.squirrel.index12306.biz.userservice.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置
 */
@Configuration
@EnableConfigurationProperties(UserRegisterBloomFilterProperties.class)
public class RBloomFilterConfiguration {

    /**
     * 防止用户注册缓存穿透的布隆过滤器
     * @param redissonClient redisson客户端
     * @param userRegisterBloomFilterProperties 用户注册布隆过滤器配置
     */
    @Bean
    public RBloomFilter<String> userRegisterCachePenetrationBloomFilter(RedissonClient redissonClient, UserRegisterBloomFilterProperties userRegisterBloomFilterProperties) {
        RBloomFilter<String> cachePenetrationBloomFilter = redissonClient.getBloomFilter(userRegisterBloomFilterProperties.getName());
        cachePenetrationBloomFilter.tryInit(
                userRegisterBloomFilterProperties.getExpectedInsertions(),// 预期插入量
                userRegisterBloomFilterProperties.getFalseProbability()// 允许错误率
        );
        return cachePenetrationBloomFilter;
    }
}
