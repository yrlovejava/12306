package com.squirrel.index12306.framework.starter.idempotent.core.spel;

import com.squirrel.index12306.framework.starter.idempotent.annotation.Idempotent;
import com.squirrel.index12306.framework.starter.idempotent.core.AbstractIdempotentExecuteHandler;
import com.squirrel.index12306.framework.starter.idempotent.core.IdempotentAspect;
import com.squirrel.index12306.framework.starter.idempotent.core.IdempotentContext;
import com.squirrel.index12306.framework.starter.idempotent.core.IdempotentParamWrapper;
import com.squirrel.index12306.framework.starter.idempotent.toolkit.SpELUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

/**
 * 基于 SpEL 方法验证请求幂等性，适用于 RestAPI 场景
 */
@RequiredArgsConstructor
public class IdempotentSpEByRestAPIExecuteHandler extends AbstractIdempotentExecuteHandler implements IdempotentSpELService {

    private final RedissonClient redissonClient;

    private final static String LOCK = "lock:spEL:restAPI";

    @SneakyThrows
    @Override
    protected IdempotentParamWrapper buildWrapper(ProceedingJoinPoint joinPoint) {
        Idempotent idempotent = IdempotentAspect.getIdempotent(joinPoint);
        String key = (String) SpELUtil.parseKey(idempotent.key(), ((MethodSignature) joinPoint.getSignature()).getMethod(), joinPoint.getArgs());
        return IdempotentParamWrapper.builder().lockKey(key).joinPoint(joinPoint).build();
    }

    /**
     * 执行幂等逻辑
     * @param wrapper 幂等参数包装器
     */
    @Override
    public void handler(IdempotentParamWrapper wrapper) {
        // 在redis中设置分布式锁
        String uniqueKey = wrapper.getIdempotent().uniqueKeyPrefix() + wrapper.getLockKey();
        RLock lock = redissonClient.getLock(uniqueKey);
        if (!lock.tryLock()) {
            return;
        }
        // 在幂等上下文中添加锁的信息
        IdempotentContext.put(LOCK, lock);
    }

    @Override
    public void postProcessing() {
        RLock lock = null;
        try {
            // 从幂等上下文中拿到锁信息
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            // 释放锁
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    @Override
    public void exceptionProcessing() {
        RLock lock = null;
        try {
            // 从幂等上下文中拿到锁信息
            lock = (RLock) IdempotentContext.getKey(LOCK);
        } finally {
            // 释放锁
            if (lock != null) {
                lock.unlock();
            }
        }
    }
}
