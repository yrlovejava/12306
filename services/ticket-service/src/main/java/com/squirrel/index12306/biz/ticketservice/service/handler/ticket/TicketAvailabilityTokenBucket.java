package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.common.enums.SeatStatusEnum;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.SeatDO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.service.SeatService;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.framework.starter.bases.Singleton;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;

/**
 * 列车车票余量令牌桶，应对海量并发场景满足并行、限流以及防超卖等场景
 */
@Component
@RequiredArgsConstructor
public final class TicketAvailabilityTokenBucket {

    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final SeatService seatService;
    private final RedissonClient redissonClient;

    private static final String LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH = "lua/ticketAvailabilityTokenBucketLua.lua";

    /**
     * 获取车站间令牌桶中的令牌访问
     * 如果返回 {@link Boolean#TRUE} 代表可以参与接下来的购票下单流程
     * 如果返回 {@link Boolean#FALSE} 代表当前访问出发站点和到达站点令牌已被拿完，无法参与购票下单等逻辑
     *
     * @param requestParam 购票请求参数入参
     * @return 是否获取列车车票余量令牌桶中的令牌，{@link Boolean#TRUE} 和 {@link Boolean#FALSE}
     */
    public boolean takeTokenFromBucket(PurchaseTicketReqDTO requestParam) {
        // 当前列车的令牌桶
        String actualHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        // 查询所有路线
        List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());

        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        Boolean hasKey = distributedCache.hasKey(actualHashKey);
        if (!hasKey) {
            // 加锁
            RLock lock = redissonClient.getLock(actualHashKey);
            lock.lock();
            try {
                // 双重检查
                Boolean hasKeyTwo = distributedCache.hasKey(actualHashKey);
                if (!hasKeyTwo) {
                    // 初始化令牌桶
                    List<Integer> seatTypes = VehicleTypeEnum.HIGH_SPEED_RAIN.getSeatTypes();
                    Map<String, String> ticketAvailabilityTokenBucketMap = new HashMap<>();
                    for (RouteDTO routeDTO : routeDTOList) {
                        for (Integer seatType : seatTypes) {
                            // 查询当前路线的座位余量
                            LambdaQueryWrapper<SeatDO> queryWrapper = Wrappers.lambdaQuery(SeatDO.class)
                                    .eq(SeatDO::getStartStation, routeDTO.getStartStation())
                                    .eq(SeatDO::getEndStation, routeDTO.getEndStation())
                                    .eq(SeatDO::getSeatType, seatType)
                                    .eq(SeatDO::getSeatStatus, SeatStatusEnum.AVAILABLE.getCode())
                                    .eq(SeatDO::getTrainId, Long.valueOf(requestParam.getTrainId()));
                            String buildCacheKey = StrUtil.join("_", routeDTO.getStartStation(), routeDTO.getEndStation(), seatType);
                            long count = seatService.count(queryWrapper);
                            ticketAvailabilityTokenBucketMap.put(buildCacheKey, String.valueOf(count));
                        }
                    }
                    stringRedisTemplate.opsForHash().putAll(TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId(), ticketAvailabilityTokenBucketMap);
                }
            }finally {
                lock.unlock();
            }
        }
        // 单例设置 lua 脚本
        DefaultRedisScript<Long> script = Singleton.get(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH)));
            redisScript.setResultType(Long.class);
            return redisScript;
        });
        Assert.notNull(script);
        // 按照座位类型分组
        Map<Integer, Long> seatTypeCountMap = requestParam.getPassengers().stream()
                .collect(Collectors.groupingBy(PurchaseTicketPassengerDetailDTO::getSeatType, Collectors.counting()));
        // 将map转为json数据 [{"seatType":1,"count":1},{"seatType":2,"count":2},{"seatType":3,"count":3}...]
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("seatType", entry.getKey());
                    jsonObject.put("count", entry.getValue());
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));
        // 拼接lua脚本参数
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());
        Long result = stringRedisTemplate.execute(script, List.of(actualHashKey, luaScriptKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(routeDTOList));
        return result != null && Objects.equals(result, 0L);
    }

    public void putTokenInBucket() {

    }

    public void initializeTokens() {

    }
}
