package com.squirrel.index12306.framework.starter.idempotent.core;

import com.squirrel.index12306.framework.starter.idempotent.annotation.Idempotent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * 幂等注解 AOP 拦截器
 */
@Aspect
public final class IdempotentAspect {

    /**
     * 增强方法标记 {@link Idempotent} 注解逻辑
     * @param joinPoint 连接点
     * @return target执行结果
     * @throws Throwable 可能抛出的异常
     */
    @Around("@annotation(com.squirrel.index12306.framework.starter.idempotent.annotation.Idempotent)")
    public Object idempotentHandler(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取 Idempotent 注解
        Idempotent idempotent = getIdempotent(joinPoint);
        IdempotentExecuteHandler instance = IdempotentExecuteHandlerFactory.getInstance(idempotent.scene(), idempotent.type());
        Object resultObj;
        try {
            instance.execute(joinPoint, idempotent);
            resultObj = joinPoint.proceed();
            instance.postProcessing();
        }catch (RepeatConsumptionException ex) {
            /**
             * 触发幂等逻辑时可能有两种情况
             *      1.消息还在梳理，但是不确定是否执行成功，那么需要返回错误，方便 RocketMQ 再次通过重试队列投递
             *      2.消息处理成功了，该消息直接返回成功即可
             */
            if(!ex.getError()){
                return null;
            }
            throw ex;
        }catch (Throwable ex) {
            // 客户端消费存在异常，需要删除幂等标识方便下次 RocketMQ 再次通过重试队列投递
            instance.exceptionProcessing();
            throw ex;
        }finally {
            IdempotentContext.clean();
        }
        return resultObj;
    }

    /**
     * 获取 Idempotent 注解
     * @param joinPoint 连接点
     * @return Idempotent注解
     * @throws NoSuchMethodException 没有这个方法异常
     */
    public static Idempotent getIdempotent(ProceedingJoinPoint joinPoint) throws NoSuchMethodException {
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(methodSignature.getName(), methodSignature.getParameterTypes());
        return targetMethod.getAnnotation(Idempotent.class);
    }
}
