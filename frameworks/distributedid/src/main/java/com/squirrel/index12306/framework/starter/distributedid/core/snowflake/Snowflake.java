package com.squirrel.index12306.framework.starter.distributedid.core.snowflake;

import cn.hutool.core.date.SystemClock;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.framework.starter.distributedid.core.IdGenerator;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * Twitter的snowflake 算法<br>
 * 分布式系统中，有一些使用全局唯一ID的场景，有一些我们希望使用一种简单一些的ID，并且希望ID能够按照时间有序生成
 *
 * <p>
 *     snowflake 的结构如下(每部分用-分开):<br>
 *     <pre>
 *     符号位（1bit）- 时间戳相对值（41bit）- 数据中心标志（5bit）- 机器标志（5bit）- 递增序号（12bit）
 *     0 - 0000000000 0000000000 0000000000 0000000000 0 - 00000 - 00000 - 000000000000
 *     </pre>
 *     <p>
 *     第一位未使用（符号位标识正数），接下来的41位为毫秒级时间(41位的长度可以使用69年)<br>
 *     然后是5位datacenterId和5位workerId(10位的长度最多支持部署1024个节点) <br>
 *     最后12位是毫秒内的计数（12位的计数顺序号支持每个节点每毫秒产生4096个ID序号）
 *     <p>
 *     并且可以通过生成的id反推出生成时间，datacenterId和workerId
 *     <p>
 *     参考：http://www.cnblogs.com/relucent/p/4955340.html<br>
 *     关于长度是18还是19的问题见：https://blog.csdn.net/unifirst/article/details/80408050
 * </p>
 */
public class Snowflake implements Serializable , IdGenerator {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 默认的起始时间，为Thu，04 Nov 2010 01:42:54
     */
    private static long DEFAULT_TWEPOCH = 1288834974657L;

    /**
     * 默认回拨时间: 2s
     */
    private static long DEFAULT_TIME_OFFSET = 2000L;

    /**
     * 机器标识: 5位
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 最大支持机器节点数0~31，一共32个
     */
    @SuppressWarnings({"FieldCanBeLocal"})
    // -1L << WORKER_ID_BITS 1111111111111111111111111111111111111111111111111111111111100000
    // 取反之后就是 0000000000000000000000000000000000000000000000000000000000011111
    // 这个值就是 WORKER_ID_BITS 位长度所能表示的最大值
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 数据中心标识: 5位
     */
    private static final long DATA_CENTER_ID_BITS = 5L;

    /**
     * 最大支持数据中心节点数0~31，一共 32 个
     * 计算方式同上
     */
    @SuppressWarnings({"FieldCanBeLocal"})
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    /**
     * 序列号12位（表示只允许workId的范围为: 0~4095）
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器节点左移12位
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心左移17位
     */
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间毫秒数左移22位
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 序列掩码，用于限定序列最大值不能超过4095
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 初始化时间点
     */
    private final long twepoch;

    private final long workerId;

    private final long dataCenterId;

    private final boolean useSystemClock;

    /**
     * 允许的时钟回拨毫秒数
     */
    private final long timeOffset;

    /**
     * 当在低频模式下时，序号始终为0，导致生成ID始终为偶数<br>
     * 此属性用于限定一个随机上限，在不同毫秒下生成序号时，给定一个随机数，避免偶数问题
     * 注意次数必须小于{@link #SEQUENCE_MASK}，{@code 0}表示不使用随机数。<br>
     * 这个上限不包括值本身
     */
    private final long randomSequenceLimit;

    /**
     * 自增序号，当高额模式下时，同一毫秒内生成N个ID，则这个序号在同一个毫秒下，自增以避免ID重复
     */
    private long sequence = 0L;

    private long lastTimestamp = -1L;

    /**
     * 构造，使用自动生成的工作节点ID和数据中心ID
     */
    public Snowflake() {
        this(IdUtil.getWorkerId(IdUtil.getDataCenterId(MAX_DATA_CENTER_ID), MAX_WORKER_ID));
    }

    /**
     * @param workerId 终端ID
     */
    public Snowflake(long workerId) {
        this(workerId, IdUtil.getDataCenterId(MAX_DATA_CENTER_ID));
    }

    /**
     * @param workerId     终端ID
     * @param dataCenterId 数据中心ID
     */
    public Snowflake(long workerId, long dataCenterId) {
        this(workerId, dataCenterId, false);
    }

    /**
     * @param workerId         终端ID
     * @param dataCenterId     数据中心ID
     * @param isUseSystemClock 是否使用{@link SystemClock} 获取当前时间戳
     */
    public Snowflake(long workerId, long dataCenterId, boolean isUseSystemClock) {
        this(null, workerId, dataCenterId, isUseSystemClock);
    }

    /**
     * @param epochDate        初始化时间起点（null表示默认起始日期）,后期修改会导致id重复,如果要修改连workerId dataCenterId，慎用
     * @param workerId         工作机器节点id
     * @param dataCenterId     数据中心id
     * @param isUseSystemClock 是否使用{@link SystemClock} 获取当前时间戳
     * @since 5.1.3
     */
    public Snowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemClock) {
        this(epochDate, workerId, dataCenterId, isUseSystemClock, DEFAULT_TIME_OFFSET);
    }

    /**
     * @param epochDate        初始化时间起点（null表示默认起始日期）,后期修改会导致id重复,如果要修改连workerId dataCenterId，慎用
     * @param workerId         工作机器节点id
     * @param dataCenterId     数据中心id
     * @param isUseSystemClock 是否使用{@link SystemClock} 获取当前时间戳
     * @param timeOffset       允许时间回拨的毫秒数
     * @since 5.8.0
     */
    public Snowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemClock, long timeOffset) {
        this(epochDate, workerId, dataCenterId, isUseSystemClock, timeOffset, 0);
    }

    /**
     * @param epochDate           初始化时间起点（null表示默认起始日期）,后期修改会导致id重复,如果要修改连workerId dataCenterId，慎用
     * @param workerId            工作机器节点id
     * @param dataCenterId        数据中心id
     * @param isUseSystemClock    是否使用{@link SystemClock} 获取当前时间戳
     * @param timeOffset          允许时间回拨的毫秒数
     * @param randomSequenceLimit 限定一个随机上限，在不同毫秒下生成序号时，给定一个随机数，避免偶数问题，0表示无随机，上限不包括值本身。
     * @since 5.8.0
     */
    public Snowflake(Date epochDate, long workerId, long dataCenterId, boolean isUseSystemClock, long timeOffset, long randomSequenceLimit) {
        this.twepoch = (null != epochDate) ? epochDate.getTime() : DEFAULT_TWEPOCH;
        this.workerId = Assert.checkBetween(workerId, 0, MAX_WORKER_ID);
        this.dataCenterId = Assert.checkBetween(dataCenterId, 0, MAX_DATA_CENTER_ID);
        this.useSystemClock = isUseSystemClock;
        this.timeOffset = timeOffset;
        this.randomSequenceLimit = Assert.checkBetween(randomSequenceLimit, 0, SEQUENCE_MASK);
    }

    /**
     * 根据Snowflake的ID，获取机器id
     *
     * @param id snowflake算法生成的id
     * @return 所属机器的id
     */
    public long getWorkerId(long id) {
        // -1L << WORKER_ID_BITS 1111111111111111111111111111111111111111111111111111111111100000
        // 取反之后就是 0000000000000000000000000000000000000000000000000000000000011111
        // id >> WORKER_ID_SHIFT 之后低12位就没有了，18 ~ 64位做&之后全为0，所以得到机器id
        return id >> WORKER_ID_SHIFT & ~(-1L << WORKER_ID_BITS);
    }

    /**
     * 根据Snowflake的ID，获取数据中心id
     *
     * @param id snowflake算法生成的id
     * @return 所属数据中心
     */
    public long getDataCenterId(long id) {
        return id >> DATA_CENTER_ID_SHIFT & ~(-1L << DATA_CENTER_ID_BITS);
    }

    /**
     * 根据Snowflake的ID，获取生成时间
     *
     * @param id snowflake算法生成的id
     * @return 生成的时间
     */
    public long getGenerateDateTime(long id) {
        return (id >> TIMESTAMP_LEFT_SHIFT & ~(-1L << 41L)) + twepoch;
    }

    /**
     * 下一个id
     * @return 雪花算法ID
     */
    @Override
    public long nextId() {
        // 获取当前的时间戳
        long timestamp = this.genTime();
        // 如果当前时间小于上次记录的时间
        if (timestamp < this.lastTimestamp) {
            // 如果回退的时间在容忍范围内
            if (this.lastTimestamp - timestamp < timeOffset) {
                // 容忍指定的回读，避免 NTP 校时造成的影响
                timestamp = lastTimestamp;
            } else {
                // 如果服务器时间有问题(时钟后退)报错
                throw new IllegalStateException(StrUtil.format("Clock moved backwards. Refusing to generate id for {}ms", lastTimestamp - timestamp));
            }
        }
        // 如果是同一毫秒
        if (timestamp == this.lastTimestamp) {
            // 序列号实现循环递增，超出最大值之后回到0
            final long sequence = (this.sequence + 1) & SEQUENCE_MASK;
            // 如果回到0，证明当前毫秒内的ID已经用完
            if (sequence == 0) {
                // 重新生成当前时间戳
                timestamp = this.tilNextMills(lastTimestamp);
            }
            this.sequence = sequence;
        } else {
            // 如果 randomSequenceLimit > 1 则初始化为一个随机值，避免序列号固定为偶数的问题
            if (randomSequenceLimit > 1) {
                sequence = RandomUtil.randomLong(randomSequenceLimit);
            } else {
                // 否则序列号初始化为0
                sequence = 0L;
            }
        }
        // 更新上次记录的时间戳
        lastTimestamp = timestamp;
        // 生成雪花算法的ID
        return ((timestamp - twepoch)) << TIMESTAMP_LEFT_SHIFT | (dataCenterId << DATA_CENTER_ID_SHIFT) | (workerId << WORKER_ID_SHIFT) | sequence;
    }

    /**
     * 下一个 ID(字符串形式)
     * @return ID 字符串形式
     */
    @Override
    public String nextIdStr() {
        return Long.toString(nextId());
    }

    /**
     * 循环等待下一个时间
     * @param lastTimestamp 上次记录的时间
     * @return 下一个时间
     */
    private long tilNextMills(long lastTimestamp) {
        // 获取当前时间时间戳
        long timestamp = this.genTime();
        // 知道获取时间戳不等于上次记录的时间戳
        // 避免同一毫秒生成多个 ID
        while (timestamp == lastTimestamp) {
            timestamp = this.genTime();
        }
        if (timestamp < lastTimestamp) {
            // 如果发现新的时间戳比上次记录的时间戳数值小，说明操作系统时间发生了倒退，报错
            throw new IllegalStateException(StrUtil.format("Clock moved backwards. Refusing to generate id for {}ms", lastTimestamp - timestamp));
        }
        // 返回时间戳
        return timestamp;
    }

    /**
     * 生成时间戳
     *
     * @return 时间戳
     */
    private long genTime() {
        // SystemClock.now() 是使用缓存值，性能高，但是可能有延迟
        // System.currentTimeMills() 直接访问系统时钟，性能较低，实时
        return this.useSystemClock ? SystemClock.now() : System.currentTimeMillis();
    }

    /**
     * 解析雪花算法生成的 ID 为对象
     * @param snowflakeId 雪花算法 ID
     * @return 雪花算法组成部分
     */
    public SnowflakeIdInfo parseSnowflakeId(long snowflakeId) {
        return SnowflakeIdInfo.builder()
                .sequence((int) (snowflakeId & ~(-1L << SEQUENCE_BITS)))
                .workerId((int)( (snowflakeId >> WORKER_ID_SHIFT) & ~(-1L << WORKER_ID_BITS) ))
                .dataCenterId((int)( (snowflakeId >> DATA_CENTER_ID_SHIFT) & ~(-1L << DATA_CENTER_ID_BITS) ))
                .timestamp(((snowflakeId >> TIMESTAMP_LEFT_SHIFT) + twepoch))
                .build();
    }
}
