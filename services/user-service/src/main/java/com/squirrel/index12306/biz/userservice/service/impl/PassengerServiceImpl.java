package com.squirrel.index12306.biz.userservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.userservice.dao.entity.PassengerDO;
import com.squirrel.index12306.biz.userservice.dao.mapper.PassengerMapper;
import com.squirrel.index12306.biz.userservice.dto.req.PassengerReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import com.squirrel.index12306.biz.userservice.service.PassengerService;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 乘车人接口实现层
 */
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerMapper passengerMapper;

    /**
     * 根据用户名查询乘车人列表
     *
     * @param username 用户名
     * @return 乘车人返回列表
     */
    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        List<PassengerDO> passengerDOList = passengerMapper.selectList(Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username));
        return BeanUtil.convert(passengerDOList, PassengerRespDTO.class);
    }

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     *
     * @param username 用户名
     * @param ids      乘车人 ID 集合
     * @return 乘车人返回列表
     */
    @Override
    public List<PassengerRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        List<PassengerDO> passengerDOList = passengerMapper.selectList(Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername, username)
                .in(PassengerDO::getId, ids));
        return BeanUtil.convert(passengerDOList, PassengerRespDTO.class);
    }

    /**
     * 新增乘车人
     *
     * @param requestParam 乘车人信息
     */
    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
        passengerDO.setCreateDate(new Date());
        passengerDO.setVerifyStatus(0);
        // TODO 用户名和当前用户比对
        passengerMapper.insert(passengerDO);
    }

    /**
     * 修改乘车人
     *
     * @param requestParam 乘车人信息
     */
    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
        LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                // TODO 用户名和当前用户比对
                .eq(PassengerDO::getUsername, requestParam.getUsername())
                .eq(PassengerDO::getId, requestParam.getId());
        passengerMapper.update(passengerDO, updateWrapper);
    }
}
