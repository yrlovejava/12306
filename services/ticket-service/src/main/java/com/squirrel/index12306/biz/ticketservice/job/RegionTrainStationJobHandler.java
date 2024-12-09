package com.squirrel.index12306.biz.ticketservice.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.RegionDO;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainStationRelationDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.RegionMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainStationRelationMapper;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.EnvironmentUtil;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.REGION_TRAIN_STATION;
import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;

/**
 * 地区站点定时查询定时任务
 */
@RestController
@RequiredArgsConstructor
public class RegionTrainStationJobHandler extends IJobHandler {

    private final RegionMapper regionMapper;
    private final TrainStationRelationMapper trainStationRelationMapper;
    private final DistributedCache distributedCache;

    @XxlJob(value = "regionTrainStationJobHandler")
    @GetMapping("/api/ticket-service/region-train-station/job/cache-init/execute")
    @Override
    public void execute() throws Exception {
        // 1.查询所有的地区站点，转换为名称的集合
        List<String> regionList = regionMapper.selectList(Wrappers.emptyWrapper())
                .stream()
                .map(RegionDO::getName)
                .toList();
        // 2.获取任务请求参数
        String requestParam = this.getJobRequestParam();
        var dateTime = StrUtil.isNotBlank(requestParam) ? requestParam : DateUtil.tomorrow().toDateStr();
        // 3.遍历查询每个站作为起始站，其他站作为终点站的结果
        for (int i = 0;i < regionList.size();i++) {
            for(int j = 0;j < regionList.size();j++) {
                if (i != j) {
                    String startRegion = regionList.get(i);
                    String endRegion = regionList.get(j);
                    List<TrainStationRelationDO> trainStationRelationDOList = trainStationRelationMapper.selectList(Wrappers.lambdaQuery(TrainStationRelationDO.class)
                            .eq(TrainStationRelationDO::getStartRegion, startRegion)
                            .eq(TrainStationRelationDO::getEndRegion, endRegion));
                    if (CollUtil.isEmpty(trainStationRelationDOList)) {
                        continue;
                    }
                    Set<ZSetOperations.TypedTuple<String>> tuples = new HashSet<>();
                    for (TrainStationRelationDO item : trainStationRelationDOList) {
                        // Redis中站点详细信息查询key: Key Prefix + 列车ID_起始站点_终点
                        String zSetKey = StrUtil.join("_", item.getTrainId(), item.getDeparture(), item.getArrival());
                        ZSetOperations.TypedTuple<String> tuple = ZSetOperations.TypedTuple.of(zSetKey, (double) item.getDepartureTime().getTime());
                        tuples.add(tuple);
                    }
                    StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
                    // Redis中站点记录的key: Key Prefix + 起始城市_终点城市_日期
                    String buildCacheKey = REGION_TRAIN_STATION + StrUtil.join("_",startRegion,endRegion,dateTime);
                    // 批处理命令加入redis
                    stringRedisTemplate.opsForZSet().add(buildCacheKey,tuples);
                    // 设置过期时间 15 天
                    stringRedisTemplate.expire(buildCacheKey,ADVANCE_TICKET_DAY, TimeUnit.DAYS);
                }
            }
        }
    }

    /**
     * 获取任务请求参数
     * @return 任务请求参数
     */
    private String getJobRequestParam() {
        return EnvironmentUtil.isDevEnvironment()
                ? ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getHeader("requestParam")
                : XxlJobHelper.getJobParam();
    }
}
