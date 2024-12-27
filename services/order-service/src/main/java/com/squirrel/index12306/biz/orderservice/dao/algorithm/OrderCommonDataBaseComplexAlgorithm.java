package com.squirrel.index12306.biz.orderservice.dao.algorithm;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import lombok.Getter;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingAlgorithm;
import org.apache.shardingsphere.sharding.api.sharding.complex.ComplexKeysShardingValue;

import java.util.*;

/**
 * 订单数据库复合分片算法配置
 */
public class OrderCommonDataBaseComplexAlgorithm implements ComplexKeysShardingAlgorithm {

    @Getter
    private Properties props;

    /**
     * 分片数量
     */
    private int shardingCount;

    private static final String SHARDING_COUNT_KEY = "sharding-count";

    /**
     * 数据库分片逻辑
     * 1.如果有user_id则使用user_id来进行分片
     * 2.如果没有user_id则使用order_sn来进行分片
     * @param availableTargetNames 目前所有可用数据库目标名称
     * @param shardingValue 包含分片键和值的复杂对象
     * @return 最终选择的目标数据库
     */
    @Override
    public Collection<String> doSharding(Collection availableTargetNames, ComplexKeysShardingValue shardingValue) {
        // 获取列名和分片值映射
        Map<String, Collection<Comparable<Long>>> columnNameAndShardingValuesMap = shardingValue.getColumnNameAndShardingValuesMap();
        // 初始化返回集合，用于存储最终选择的目标数据库，使用LinkedHashSet保持顺序并去重
        Collection<String> result = new LinkedHashSet<>(availableTargetNames.size());
        if (CollUtil.isNotEmpty(columnNameAndShardingValuesMap)) {
            String userId = "user_id";
            // 获取 user_id 的分片值集合
            Collection<Comparable<Long>> customerUserIdCollection = columnNameAndShardingValuesMap.get(userId);
            // 如果集合非空
            if (CollUtil.isNotEmpty(customerUserIdCollection)) {
                // 获取第一个分片值
                Comparable<?> comparable = customerUserIdCollection.stream().findFirst().get();
                // 如果这个分片值是String类型
                if (comparable instanceof String) {
                    String actualOrderSn = comparable.toString();
                    // 取 actualOrderSn 的最后几位数来进行哈希计算
                    // 结果对分片总数 shardingCount 取模，得到数据库的后缀
                    result.add("ds_" + this.hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0))) % shardingCount);
                } else {
                    // comparable 是Long类型，则进行模1000000操作，并进行哈希计算，然后取模分片数得到数据库后缀
                    String dbSuffix = String.valueOf(this.hashShardingValue((Long) comparable % 1000000) % shardingCount);
                    result.add("ds_" + dbSuffix);
                }
            } else {
                // 如果user_id没有，则使用order_sn进行分片
                String orderSn = "order_sn";
                Collection<Comparable<Long>> orderSnCollection = columnNameAndShardingValuesMap.get(orderSn);
                Comparable<?> comparable = orderSnCollection.stream().findFirst().get();
                if (comparable instanceof String) {
                    String actualOrderSn = comparable.toString();
                    result.add("ds_" + hashShardingValue(actualOrderSn.substring(Math.max(actualOrderSn.length() - 6, 0))) % shardingCount);
                } else {
                    result.add("ds_" + hashShardingValue((Long) comparable % 1000000) % shardingCount);
                }
            }
        }
        return result;
    }

    /**
     * 初始化配置
     * @param props 配置
     */
    @Override
    public void init(Properties props) {
        this.props = props;
        shardingCount = this.getShardingCount(props);
    }

    /**
     * 获取分片数量
     * @param props 配置
     * @return 分片数量
     */
    private int getShardingCount(final Properties props){
        Preconditions.checkArgument(props.containsKey(SHARDING_COUNT_KEY),"Sharding count cannot be null");
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    /**
     * 计算哈希值
     * @param shardingValue 分片键值的value
     * @return 哈希值
     */
    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }
}
