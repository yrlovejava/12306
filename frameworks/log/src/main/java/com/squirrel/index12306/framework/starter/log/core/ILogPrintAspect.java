package com.squirrel.index12306.framework.starter.log.core;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.SystemClock;
import com.alibaba.fastjson2.JSON;
import com.squirrel.index12306.framework.starter.log.annotation.ILog;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * {@link ILog} 日志打印 AOP 切面
 */
@Aspect
public class ILogPrintAspect {

    /**
     * 打印类或方法上的 {@link ILog}
     *
     * @param joinPoint 连接点
     * @return target执行结果
     * @throws Throwable 可能抛出的异常
     */
    // 环绕通知
    @Around("@within(com.squirrel.index12306.framework.starter.log.annotation.ILog) || @annotation(com.squirrel.index12306.framework.starter.log.annotation.ILog)")
    public Object printMLog(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取当前时间戳
        long startTime = SystemClock.now();
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        // 获取日志打印器
        Logger log = LoggerFactory.getLogger(methodSignature.getDeclaringType());
        String beginTime = DateUtil.now();
        // 原始代码的执行结果
        Object result = null;
        try {
            result = joinPoint.proceed();
        } finally {
            // 获取实际执行的原始方法
            Method targetMethod = joinPoint.getTarget().getClass().getDeclaredMethod(
                    methodSignature.getName(),// 方法名
                    methodSignature.getMethod().getParameterTypes()// 参数类型
            );
            // 获取自定义ILog注解
            // 要么在方法上，要么在类上
            ILog logAnnotation = Optional.ofNullable(targetMethod.getAnnotation(ILog.class))
                    .orElse(joinPoint.getTarget().getClass().getAnnotation(ILog.class));
            if (logAnnotation != null) {
                ILogPrintDTO logPrint = new ILogPrintDTO();
                logPrint.setBeginTime(beginTime);// 开始执行时间
                // 如果打印结果要包含入参
                if (logAnnotation.input()) {
                    logPrint.setInputParams(this.buildInput(joinPoint));
                }
                // 如果打印结果要包含出参
                if (logAnnotation.output()) {
                    logPrint.setOutputParams(result);
                }
                String methodType = "", requestURI = "";
                try {
                    // 获取请求参数
                    ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    assert servletRequestAttributes != null;
                    // 方法名
                    methodType = servletRequestAttributes.getRequest().getMethod();
                    // 请求路径
                    requestURI = servletRequestAttributes.getRequest().getRequestURI();
                } catch (Exception ignored) {
                }
                log.info("[{}] {}, executeTime: {}ms, info: {}", methodType, requestURI, SystemClock.now() - startTime, JSON.toJSONString(logPrint));
            }
        }
        return result;
    }

    /**
     * 构造打印的入参
     *
     * @param joinPoint 连接点
     * @return 打印的入参
     */
    private Object[] buildInput(ProceedingJoinPoint joinPoint) {
        // 获取参数
        Object[] args = joinPoint.getArgs();
        Object[] printArgs = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            // 忽略http请求和http响应
            if ((args[i] instanceof HttpServletRequest) || args[i] instanceof HttpServletResponse) {
                continue;
            }
            if (args[i] instanceof byte[]) {
                // byte[] 数组
                printArgs[i] = "byte array";
            } else if (args[i] instanceof MultipartFile) {
                // 文件上传
                printArgs[i] = "file";
            } else {
                printArgs[i] = args[i];
            }
        }
        return printArgs;
    }
}
