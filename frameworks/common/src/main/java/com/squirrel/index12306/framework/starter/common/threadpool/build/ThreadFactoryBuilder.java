package com.squirrel.index12306.framework.starter.common.threadpool.build;

import com.squirrel.index12306.framework.starter.designpattern.builder.Builder;

import java.io.Serial;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程工厂 {@link ThreadFactory} 构建器，构建者模式
 */
public final class ThreadFactoryBuilder implements Builder<ThreadFactory> {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 线程工厂
     */
    private ThreadFactory backingThreadFactory;

    /**
     * 线程名前缀
     */
    private String namePrefix;

    /**
     * 是否为守护线程
     */
    private Boolean daemon;

    /**
     * 优先级
     */
    private Integer priority;

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

    public ThreadFactoryBuilder threadFactory(ThreadFactory backingThreadFactory) {
        this.backingThreadFactory = backingThreadFactory;
        return this;
    }

    public ThreadFactoryBuilder prefix(String prefix) {
        this.namePrefix = prefix;
        return this;
    }

    public ThreadFactoryBuilder daemon(Boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public ThreadFactoryBuilder priority(int priority) {
        if (priority < Thread.MIN_PRIORITY) {
            throw new IllegalArgumentException(String.format("Thread priority (%d) must be >= %d", priority, Thread.MIN_PRIORITY));
        }
        if (priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(String.format("Thread priority (%d) must be <= %d", priority, Thread.MAX_PRIORITY));
        }
        this.priority = priority;
        return this;
    }

    public void uncaughtExceptionHandler(Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = uncaughtExceptionHandler;
    }

    public static ThreadFactoryBuilder builder() {
        return new ThreadFactoryBuilder();
    }

    @Override
    public ThreadFactory build() {
        return build(this);
    }

    /**
     * 建造方法
     * @param builder 各种属性
     * @return 线程工厂
     */
    private static ThreadFactory build(ThreadFactoryBuilder builder) {
        // final 关键字确保局部变量被赋值后，无法被修改，避免不一致的线程配置
        final ThreadFactory backingThreadFactory = (null != builder.backingThreadFactory)
                ? builder.backingThreadFactory
                : Executors.defaultThreadFactory();
        final String namePrefix = builder.namePrefix;
        final Boolean daemon = builder.daemon;
        final Integer priority = builder.priority;
        final Thread.UncaughtExceptionHandler handler = builder.uncaughtExceptionHandler;
        final AtomicLong count = (null == namePrefix) ? null : new AtomicLong();
        return r -> {
            final Thread thread = backingThreadFactory.newThread(r);
            if (null != namePrefix) {
                thread.setName(namePrefix + "_" + count.getAndIncrement());
            }
            if (null != daemon) {
                thread.setDaemon(daemon);
            }
            if (null != priority) {
                thread.setPriority(priority);
            }
            if (null != handler) {
                thread.setUncaughtExceptionHandler(handler);
            }
            return thread;
        };
    }
}
