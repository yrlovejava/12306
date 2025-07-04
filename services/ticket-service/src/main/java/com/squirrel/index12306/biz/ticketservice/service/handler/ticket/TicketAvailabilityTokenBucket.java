package com.squirrel.index12306.biz.ticketservice.service.handler.ticket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import com.squirrel.index12306.biz.ticketservice.common.enums.VehicleTypeEnum;
import com.squirrel.index12306.biz.ticketservice.dao.entity.TrainDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.SeatMapper;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.TrainMapper;
import com.squirrel.index12306.biz.ticketservice.dto.domain.PurchaseTicketPassengerDetailDTO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.RouteDTO;
import com.squirrel.index12306.biz.ticketservice.dto.domain.SeatTypeCountDTO;
import com.squirrel.index12306.biz.ticketservice.dto.req.PurchaseTicketReqDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.remote.dto.TicketOrderPassengerDetailRespDTO;
import com.squirrel.index12306.biz.ticketservice.service.TrainStationService;
import com.squirrel.index12306.framework.starter.bases.Singleton;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import com.squirrel.index12306.framework.starter.common.toolkit.Assert;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.Index12306Constant.ADVANCE_TICKET_DAY;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TICKET_AVAILABILITY_TOKEN_BUCKET;
import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_INFO;

/**
 * 列车车票余量令牌桶，应对海量并发场景满足并行、限流以及防超卖等场景
 */
@Slf4j
@Component
@RequiredArgsConstructor
public final class TicketAvailabilityTokenBucket {

    private final TrainStationService trainStationService;
    private final DistributedCache distributedCache;
    private final RedissonClient redissonClient;
    private final SeatMapper seatMapper;
    private final TrainMapper trainMapper;

    private static final String LUA_TICKET_AVAILABILITY_TOKEN_BUCKET_PATH = "lua/ticket_availability_token_bucket.lua";
    private static final String LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH = "lua/ticket_availability_rollback_token_bucket.lua";

    /**
     * 获取车站间令牌桶中的令牌访问
     * 如果返回 {@link Boolean#TRUE} 代表可以参与接下来的购票下单流程
     * 如果返回 {@link Boolean#FALSE} 代表当前访问出发站点和到达站点令牌已被拿完，无法参与购票下单等逻辑
     *
     * @param requestParam 购票请求参数入参
     * @return 是否获取列车车票余量令牌桶中的令牌，{@link Boolean#TRUE} 和 {@link Boolean#FALSE}
     */
    public boolean takeTokenFromBucket(PurchaseTicketReqDTO requestParam) {
        // 从缓存中获取列车信息
        TrainDO trainDO = distributedCache.safeGet(
                TRAIN_INFO + requestParam.getTrainId(),
                TrainDO.class,
                () -> {
                    // 从数据库中获取列车信息
                    return trainMapper.selectById(requestParam.getTrainId());
                },
                ADVANCE_TICKET_DAY,
                TimeUnit.DAYS);
        // 查询所有路线
        List<RouteDTO> routeDTOList = trainStationService.listTrainStationRoute(requestParam.getTrainId(), trainDO.getStartStation(), trainDO.getEndStation());
        // 当前列车的令牌桶
        String actualHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
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
                        List<SeatTypeCountDTO> seatTypeCountDTOList = seatMapper.listSeatTypeCount(Long.parseLong(requestParam.getTrainId()), routeDTO.getStartStation(), routeDTO.getEndStation(), seatTypes);

                        for (SeatTypeCountDTO each : seatTypeCountDTOList) {
                            String buildCacheKey = StrUtil.join("_", routeDTO.getStartStation(), routeDTO.getEndStation(), each.getSeatType());
                            ticketAvailabilityTokenBucketMap.put(buildCacheKey, String.valueOf(each.getSeatCount()));
                        }
                    }
                    stringRedisTemplate.opsForHash().putAll(TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId(), ticketAvailabilityTokenBucketMap);
                }
            } finally {
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
        // 查询所有需要扣减的路线
        List<RouteDTO> takeoutRouteDTOList = trainStationService
                .listTakeoutTrainStationRoute(requestParam.getTrainId(), requestParam.getDeparture(), requestParam.getArrival());
        // 拼接lua脚本参数
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());
        Long result = stringRedisTemplate.execute(script, List.of(actualHashKey, luaScriptKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(routeDTOList));
        return result != null && Objects.equals(result, 0L);
    }

    /**
     * 回滚列车余量令牌，一般为订单取消或长时间按未支付触发
     *
     * @param requestParam 订单详细信息
     */
    public void rollbackInBucket(TicketOrderDetailRespDTO requestParam) {
        // 单例获取 LUA 执行脚本
        DefaultRedisScript<Long> actual = Singleton.get(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH, () -> {
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(LUA_TICKET_AVAILABILITY_ROLLBACK_TOKEN_BUCKET_PATH)));
            redisScript.setResultType(Long.class);
            return redisScript;
        });
        Assert.notNull(actual);

        // 拼接 LUA 脚本的参数
        // 获取座位类型对应的数量
        List<TicketOrderPassengerDetailRespDTO> passengerDetails = requestParam.getPassengerDetails();
        Map<Integer, Long> seatTypeCountMap = passengerDetails.stream()
                .collect(Collectors.groupingBy(TicketOrderPassengerDetailRespDTO::getSeatType, Collectors.counting()));
        JSONArray seatTypeCountArray = seatTypeCountMap.entrySet().stream()
                .map(entry -> {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("seatType", String.valueOf(entry.getKey()));
                    jsonObject.put("count", String.valueOf(entry.getValue()));
                    return jsonObject;
                })
                .collect(Collectors.toCollection(JSONArray::new));
        // 获取 redisTemplate
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        // 拼接令牌桶 key
        String actualHashKey = TICKET_AVAILABILITY_TOKEN_BUCKET + requestParam.getTrainId();
        // 拼接 hashKey
        String luaScriptKey = StrUtil.join("_", requestParam.getDeparture(), requestParam.getArrival());
        // 查询所有需要扣减的路线
        List<RouteDTO> takeoutRouteDTOList = trainStationService.
                listTakeoutTrainStationRoute(String.valueOf(requestParam.getTrainId()), requestParam.getDeparture(), requestParam.getArrival());
        Long result = stringRedisTemplate.execute(actual, Lists.newArrayList(actualHashKey, luaScriptKey), JSON.toJSONString(seatTypeCountArray), JSON.toJSONString(takeoutRouteDTOList));
        if (result == null || !Objects.equals(result, 0L)) {
            log.error("回滚列车余票令牌失败，订单信息：{}", JSON.toJSONString(requestParam));
            throw new ServiceException("回滚列车余票令牌失败");
        }
    }

    public void putTokenInBucket() {

    }

    public void initializeTokens() {

    }
}
