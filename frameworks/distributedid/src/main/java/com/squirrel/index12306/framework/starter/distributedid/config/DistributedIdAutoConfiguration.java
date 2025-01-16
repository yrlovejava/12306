package com.squirrel.index12306.framework.starter.distributedid.config;

import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import com.squirrel.index12306.framework.starter.distributedid.core.snowflake.LocalRedisWorkIdChoose;
import com.squirrel.index12306.framework.starter.distributedid.core.snowflake.RandomWorkIdChoose;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 分布式 ID 自动装配
 */
@Import(ApplicationContextHolder.class)
public class DistributedIdAutoConfiguration {

    /**
     * 本地 Redis 构建雪花算法 WorkId 选择器
     * @return 雪花算法ID
     */
    @Bean
    @ConditionalOnProperty("spring.data.redis.host")
    public LocalRedisWorkIdChoose redisWorkIdChoose() {
        return new LocalRedisWorkIdChoose();
    }

    /**
     * 随机数构建雪花 WorkId 选择器。如果项目未使用 Redis，使用该选择器
     * @return 雪花算法ID
     */
    @Bean
    @ConditionalOnMissingBean
    public RandomWorkIdChoose randomWorkIdChoose() {
        return new RandomWorkIdChoose();
    }

}
