package com.squirrel.index12306.biz.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.biz.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 列车站点余票定时任务
 * 已通过运行时实时获取解决该定时任务
 */
@Deprecated
@RestController
@RequiredArgsConstructor
public class TrainStationRemainingTicketJobHandler extends AbstractTrainStationJobHandlerTemplate {

    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;
    private final TrainMapper trainMapper;

    /**
     * 为了方便使用项目启动的时候初始化缓存
     * 注意: 生产环境不会这么操作，因为生产环境一般采用滚动发布，如果直接赋值可能会出现问题
     */
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
                Long trainId = item.getTrainId();
                TrainDO trainDO = trainMapper.selectById(trainId);
                Map<String, String> trainStationRemainingTicket = new HashMap<>();
                switch (trainDO.getTrainType()) {
                    case 0 -> {
                        trainStationRemainingTicket.put("0", "10");
                        trainStationRemainingTicket.put("1", "140");
                        trainStationRemainingTicket.put("2", "810");
                    }
                    case 1 -> {
                        trainStationRemainingTicket.put("3", "96");
                        trainStationRemainingTicket.put("4", "192");
                        trainStationRemainingTicket.put("5", "216");
                        trainStationRemainingTicket.put("13", "216");
                    }
                    case 2 -> {
                        trainStationRemainingTicket.put("6", "96");
                        trainStationRemainingTicket.put("7", "192");
                        trainStationRemainingTicket.put("8", "216");
                        trainStationRemainingTicket.put("13", "216");
                    }
                }
                StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                // Redis中列车余票key: Key Prefix + 列车ID_起始站点_终点
                String buildCacheKey = TRAIN_STATION_REMAINING_TICKET + StrUtil.join("_", each.getId(), item.getDeparture(), item.getArrival());
                stringRedisTemplate.opsForHash().putAll(buildCacheKey, trainStationRemainingTicket);
                // 设置过期时间
                stringRedisTemplate.expire(buildCacheKey, ADVANCE_TICKET_DAY, TimeUnit.DAYS);
            }
        }
    }
}
