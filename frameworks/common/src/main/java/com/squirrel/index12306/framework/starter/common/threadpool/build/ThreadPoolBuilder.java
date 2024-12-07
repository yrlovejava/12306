package com.squirrel.index12306.framework.starter.common.threadpool.build;

import com.squirrel.index12306.framework.starter.common.toolkit.Assert;
import com.squirrel.index12306.framework.starter.designpattern.builder.Builder;

import java.math.BigDecimal;
import java.util.concurrent.*;

/**
 * 线程池 {@link ThreadPoolExecutor} 构建器，构建者模式
 */
public final class ThreadPoolBuilder implements Builder<ThreadPoolExecutor> {

    /**
     * 核心线程数
     */
    private int corePoolSize = this.calculateCoreNum();

    /**
     * 最大线程数
     */
    private int maximumPoolSize = corePoolSize + (corePoolSize >> 1);

    /**
     * 空闲线程存活时间
     */
    private long keepAliveTime = 30000L;

    /**
     * 时间单位
     */
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    /**
     * 阻塞队列
     */
    private BlockingQueue<Runnable> workQueue = new LinkedBlockingDeque<>(4096);

    /**
     * 拒绝策略
     */
    private RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.AbortPolicy();

    /**
     * 是否为守护线程
     */
    private boolean isDaemon = false;

    /**
     * 线程名前缀
     */
    private String threadNamePrefix;

    /**
     * 线程工厂
     */
    private ThreadFactory threadFactory;

    /**
     * 计算核心线程数
     * @return 核心线程数
     */
    private Integer calculateCoreNum() {
        // 获取处理器数量
        int cpuCoreNum = Runtime.getRuntime().availableProcessors();
        // 核心线程数 = 处理器数量 / 0.2
        return new BigDecimal(cpuCoreNum).divide(new BigDecimal("0.2")).intValue();
    }

    public ThreadPoolBuilder threadFactory(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
        return this;
    }

    public ThreadPoolBuilder corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public ThreadPoolBuilder maximumPoolSize(int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        if (maximumPoolSize < this.corePoolSize) {
            this.corePoolSize = maximumPoolSize;
        }
        return this;
    }

    public ThreadPoolBuilder threadFactory(String threadNamePrefix, Boolean isDaemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.isDaemon = isDaemon;
        return this;
    }

    public ThreadPoolBuilder keepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
        return this;
    }

    public ThreadPoolBuilder keepAliveTime(long keepAliveTime, TimeUnit timeUnit) {
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        return this;
    }

    public ThreadPoolBuilder rejected(RejectedExecutionHandler rejectedExecutionHandler) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        return this;
    }

    public ThreadPoolBuilder workQueue(BlockingQueue workQueue) {
        this.workQueue = workQueue;
        return this;
    }

    public static ThreadPoolBuilder builder() {
        return new ThreadPoolBuilder();
    }

    @Override
    public ThreadPoolExecutor build() {
        if (threadFactory == null) {
            Assert.notEmpty(threadNamePrefix,"The thread name prefix cannot be empty or an empty string.");
            threadFactory = ThreadFactoryBuilder.builder().prefix(threadNamePrefix).daemon(isDaemon).build();
        }
        ThreadPoolExecutor executorService;
        try {
            executorService = new ThreadPoolExecutor(corePoolSize,
                    maximumPoolSize,
                    keepAliveTime,
                    timeUnit,
                    workQueue,
                    threadFactory,
                    rejectedExecutionHandler);
        }catch (IllegalStateException ex) {
            throw new IllegalArgumentException("Error creating thread pool parameter.",ex);
        }
        return executorService;
    }
}
