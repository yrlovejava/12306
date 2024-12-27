package com.squirrel.index12306.biz.orderservice.service.orderid;

/**
 * 全局唯一订单号生成器
 */
public class DistributedIdGenerator {

    private static final long EPOCH = 1609459200000L;
    private static final int NODE_BITS = 5;
    private static final int SEQUENCE_BITS = 7;

    private final long nodeID;
    private long lastTimestamp = -1L;
    private long sequence = 0L;

    public DistributedIdGenerator(long nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * 生成全局唯一订单号
     * @return 订单号
     */
    public synchronized long generatedId() {
        // 计算时间
        long timestamp = System.currentTimeMillis() - EPOCH;
        // 检查时间回退
        if(timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }
        // 如果在同一毫秒内生成了多个请求ID
        if(timestamp == lastTimestamp){
            // sequence 自增
            // & ((1 << SEQUENCE_BITS) - 1) 进行掩码操作，确保sequence不会超过该位数的最大值
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);
            // 如果自增后等于0，那么说明同一毫秒内生成了太多ID
            if(sequence == 0){
                // 这时候需要等到下一毫秒才能生成新的ID
                timestamp = this.tilNextMillis(lastTimestamp);
            }
        }else {
            // 重置序列号
            sequence = 0L;
        }
        lastTimestamp = timestamp;
        // 生成最终的ID
        return (timestamp << (NODE_BITS + SEQUENCE_BITS)) | (nodeID << SEQUENCE_BITS) | sequence;
    }

    /**
     * 等待下一毫秒
     * @param lastTimestamp 上一次获取的时间戳
     * @return 下一毫秒的时间戳
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis() - EPOCH;
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        return timestamp;
    }
}
