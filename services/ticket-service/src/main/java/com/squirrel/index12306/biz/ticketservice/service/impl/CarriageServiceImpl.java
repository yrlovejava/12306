package com.squirrel.index12306.biz.ticketservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.ticketservice.dao.entity.CarriageDO;
import com.squirrel.index12306.biz.ticketservice.dao.mapper.CarriageMapper;
import com.squirrel.index12306.biz.ticketservice.service.CarriageService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import static com.squirrel.index12306.biz.ticketservice.common.constant.RedisKeyConstant.TRAIN_CARRIAGE;

/**
 * 列车车厢接口层实现
 */
@Service
@RequiredArgsConstructor
public class CarriageServiceImpl implements CarriageService {

    private final DistributedCache distributedCache;
    private final CarriageMapper carriageMapper;

    /**
     * 查询列车车厢号集合
     *
     * @param trainId      列车 ID
     * @param carriageType 车厢类型
     * @return 车厢号集合
     */
    @Override
    public List<String> listCarriageNumber(String trainId, Integer carriageType) {
        // 从Redis中获取车厢号，车厢信息在Redis中哈希结构存储
        StringRedisTemplate stringRedisTemplate = (StringRedisTemplate) distributedCache.getInstance();
        Object trainCarriageStr = stringRedisTemplate.opsForHash().get(TRAIN_CARRIAGE + trainId, String.valueOf(carriageType));
        // 如果Redis中没有，需要从数据库中查询
        if (trainCarriageStr == null) {
            List<CarriageDO> carriageDOList = carriageMapper.selectList(Wrappers.lambdaQuery(CarriageDO.class)
                    .eq(CarriageDO::getTrainId, trainId)
                    .eq(CarriageDO::getCarriageType, carriageType));
            return carriageDOList.stream().map(CarriageDO::getCarriageNumber).collect(Collectors.toList());
        }
        return StrUtil.split(trainCarriageStr.toString(),",");
    }
}
