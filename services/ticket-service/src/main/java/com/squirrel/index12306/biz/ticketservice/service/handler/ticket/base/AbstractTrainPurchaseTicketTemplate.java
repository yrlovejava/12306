package com.squirrel.index12306.biz.ticketservice.service.handler.ticket.base;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.SelectSeatDTO;
import com.squirrel.index12306.biz.ticketservice.service.handler.ticket.dto.TrainPurchaseTicketRespDTO;
import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.designpattern.stategy.AbstractExecuteStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_STATION_REMAINING_TICKET;

/**
 * 抽象高铁购票模板基础服务
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractTrainPurchaseTicketTemplate implements CommandLineRunner, IPurchaseTicket, AbstractExecuteStrategy<SelectSeatDTO, List<TrainPurchaseTicketRespDTO>> {

    private DistributedCache distributedCache;
    private String ticketAvailabilityCacheUpdateType;

    /**
     * 执行策略，这里是购票策略
     * @param requestParam 购票请求参数
     * @return List<TrainPurchaseTicketRespDTO> 乘车人和对应的座位
     */
    @Override
    public List<TrainPurchaseTicketRespDTO> executeResp(SelectSeatDTO requestParam) {
        // TODO 后续逻辑全部转换为 LUA 缓存原子操作
        // 选择座位，有子类具体实现
        List<TrainPurchaseTicketRespDTO> actualResult = selectSeats(requestParam);
        // 扣减车厢余票缓存，扣减站点余票缓存
        if(CollUtil.isNotEmpty(actualResult) && !StrUtil.equals(ticketAvailabilityCacheUpdateType, "binlog")){
            // 列车id
            String trainId = requestParam.getRequestParam().getTrainId();
            // 出发站
            String departure = requestParam.getRequestParam().getDeparture();
            // 到达站
            String arrival = requestParam.getRequestParam().getArrival();
            // redis中车辆余票key后缀: 列车ID_出发站_到达站
            String keySuffix = StrUtil.join("_", trainId, departure, arrival);
            StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
            stringRedisTemplate.opsForHash().increment(TRAIN_STATION_REMAINING_TICKET + keySuffix, String.valueOf(requestParam.getSeatType()), -actualResult.size());
        }
        return actualResult;
    }

    /**
     * 选择座位
     *
     * @param requestParam 购票请求入参
     * @return 乘车人座位
     */
    protected abstract List<TrainPurchaseTicketRespDTO> selectSeats(SelectSeatDTO requestParam);

    /**
     * 应用启动后执行初始化任务
     * @param args 参数
     * @throws Exception 可能抛出异常
     */
    @Override
    public void run(String... args) throws Exception {
        distributedCache = ApplicationContextHolder.getBean(DistributedCache.class);
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);
        ticketAvailabilityCacheUpdateType = configurableEnvironment.getProperty("ticket-availability.cache-update.type", "");
    }
}