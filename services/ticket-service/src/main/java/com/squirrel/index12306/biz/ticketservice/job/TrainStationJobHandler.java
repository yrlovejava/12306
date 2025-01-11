package com.squirrel.index12306.biz.ticketservice.job;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationMapper;
import com.squirrel.index12306.biz.ticketservice.job.base.AbstractTrainStationJobHandlerTemplate;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_STOPOVER_DETAIL;

/**
 * 列车路线信息定时任务
 * 已通过运行时试试获取解决该定时任务
 */
@Deprecated
@RestController
@RequiredArgsConstructor
public class TrainStationJobHandler extends AbstractTrainStationJobHandlerTemplate {

    private final TrainStationMapper trainStationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "trainStationJobHandler")
    @GetMapping("/api/ticket-service/train-station/job/cache-init/execute")
    @Override
    public void execute() {
        super.execute();
    }


    @Override
    protected void actualExecute(List<TrainDO> trainDOPageRecords) {
        for (TrainDO each : trainDOPageRecords) {
            // 查询列车经过的站点
            List<TrainStationDO> trainStationDOList = trainStationMapper.selectList(Wrappers.lambdaQuery(TrainStationDO.class)
                    .eq(TrainStationDO::getTrainId, each.getId())
            );
            // Redis中列车路线信息key: Key Prefix + 列车ID
            distributedCache.put(
                    TRAIN_STATION_STOPOVER_DETAIL + each.getId(),
                    JSON.toJSONString(trainStationDOList),
                    Index12306Constant.ADVANCE_TICKET_DAY,
                    TimeUnit.DAYS
            );
        }
    }
}
