package com.squirrel.index12306.biz.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.biz.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 列车站点余票定时任务
 */
@RestController
@RequiredArgsConstructor
public class TrainStationRemainingTicketJobHandler extends AbstractTrainStationJobHandlerTemplate {

    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "trainStationRemainingTicketJobHandler")
    @GetMapping("/api/ticket-service/train-station-remaining-ticket/job/cache-init/execute")
    @Override
    public void execute() {
        super.execute();
    }

    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        for (TrainDO each : trainDOPageRecords) {
            // 查询列车站点关系
            LambdaQueryWrapper<TrainStationRelationDO> relationQueryWrapper = Wrappers.lambdaQuery(TrainStationRelationDO.class)
                    .eq(TrainStationRelationDO::getTrainId, each.getId());
            List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(relationQueryWrapper);
            if (CollUtil.isEmpty(trainStationRelationDOList)) {
                return;
            }
            // 遍历站点关系，给列车不同座位票设置余票
            for (TrainStationRelationDO item : trainStationRelationDOList) {
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // TODO 需要考虑动态库存逻辑&列车类型
                Map<String, String> trainStationRemainingTicket = MapUtil.builder("0", "10")
                        .put("1", "140")
                        .put("2", "810")
                        .build();
                // Redis中列车余票key: Key Prefix + 列车ID_起始站点_终点
                String buildCacheKey = TRAIN_STATION_REMAINING_TICKET + StrUtil.join("_", each.getId(), item.getDeparture(), item.getArrival());
                stringRedisTemplate.opsForHash().putAll(buildCacheKey, trainStationRemainingTicket);
                // 设置过期时间
                stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
            }
        }
    }
}
