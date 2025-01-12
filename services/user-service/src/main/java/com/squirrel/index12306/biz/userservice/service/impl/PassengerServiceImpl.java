package com.squirrel.index12306.biz.userservice.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.squirrel.index12306.biz.userservice.common.enums.VerifyStatusEnum;
import com.squirrel.index12306.biz.userservice.dao.entity.PassengerDO;
import com.squirrel.index12306.biz.userservice.dao.mapper.PassengerMapper;
import com.squirrel.index12306.biz.userservice.dto.req.PassengerRemoveReqDTO;
import com.squirrel.index12306.biz.userservice.dto.req.PassengerReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.PassengerActualRespDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import com.squirrel.index12306.biz.userservice.service.PassengerService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.frameworks.starter.user.core.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.userservice.common.constant.RedisKeyConstant.USER_PASSENGER_LIST;

/**
 * 乘车人接口实现层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PassengerServiceImpl implements PassengerService {

    private final PassengerMapper passengerMapper;
    private final PlatformTransactionManager transactionManager;
    private final DistributedCache distributedCache;

    /**
     * 根据用户名查询乘车人列表
     *
     * @param username 用户名
     * @return 乘车人返回列表
     */
    @Override
    public List<PassengerRespDTO> listPassengerQueryByUsername(String username) {
        // 在redis中获取乘车人信息，如果没有从数据库中查询并添加到redis中
        String actualUserPassengerListStr = distributedCache.safeGet(
                USER_PASSENGER_LIST + username,
                String.class,
                () -> {
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username));
                    return CollUtil.isNotEmpty(passengerDOList) ? JSON.toJSONString(passengerDOList) : null;
                },
                1,
                TimeUnit.DAYS
        );
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(each -> JSON.parseArray(each,PassengerDO.class))
                .map(each -> BeanUtil.convert(each, PassengerRespDTO.class))
                .orElse(null);
    }

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     *
     * @param username 用户名
     * @param ids      乘车人 ID 集合
     * @return 乘车人返回列表
     */
    @Override
    public List<PassengerActualRespDTO> listPassengerQueryByIds(String username, List<Long> ids) {
        // 在redis中获取乘车人信息，如果没有从数据库中查询并添加到redis中
        String actualUserPassengerListStr = distributedCache.safeGet(
                USER_PASSENGER_LIST + username,
                String.class,
                () -> {
                    List<PassengerDO> passengerDOList = passengerMapper.selectList(Wrappers.lambdaQuery(PassengerDO.class)
                            .eq(PassengerDO::getUsername, username)
                            .in(PassengerDO::getId, ids));
                    return CollUtil.isNotEmpty(passengerDOList) ? JSON.toJSONString(passengerDOList) : null;
                },
                1,
                TimeUnit.DAYS
        );
        return Optional.ofNullable(actualUserPassengerListStr)
                .map(each -> JSON.parseArray(each,PassengerDO.class))
                .map(each -> BeanUtil.convert(each, PassengerActualRespDTO.class))
                .orElse(null);
    }

    /**
     * 新增乘车人
     *
     * @param requestParam 乘车人信息
     */
    @Override
    public void savePassenger(PassengerReqDTO requestParam) {
        // 创建一个默认事务定义
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        // 通过事务管理器获取一个事务状态
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        // 获取当前用户名
        String username = UserContext.getUsername();
        try {
            // 解析乘车人信息
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            passengerDO.setCreateDate(new Date());
            passengerDO.setVerifyStatus(VerifyStatusEnum.REVIEWED.getCode());
            // 插入数据库
            int inserted = passengerMapper.insert(passengerDO);
            if (!SqlHelper.retBool(inserted)){
                throw new ServiceException(String.format("[%s] 新增乘车人失败", username));
            }
            // 手动提交事务
            transactionManager.commit(transactionStatus);
        }catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 新增乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            // 手动回滚事务
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        // 删除缓存
        delUserPassengerCache(username);
    }

    /**
     * 修改乘车人
     *
     * @param requestParam 乘车人信息
     */
    @Override
    public void updatePassenger(PassengerReqDTO requestParam) {
        // 创建一个默认事务定义
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        // 通过事务管理器获取一个事务状态
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        // 获取当前用户名
        String username = UserContext.getUsername();
        try {
            // 解析乘车人信息
            PassengerDO passengerDO = BeanUtil.convert(requestParam, PassengerDO.class);
            passengerDO.setUsername(username);
            LambdaUpdateWrapper<PassengerDO> updateWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            // 修改数据库中数据
            int updated = passengerMapper.update(passengerDO, updateWrapper);
            if (!SqlHelper.retBool(updated)){
                throw new ServiceException(String.format("[%s] 修改乘车人失败", username));
            }
            // 手动提交事务
            transactionManager.commit(transactionStatus);
        }catch (Exception ex){
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 修改乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            // 手动回滚事务
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        // 删除缓存
        delUserPassengerCache(username);
    }

    /**
     * 移除乘车人
     *
     * @param requestParam 移除乘车人信息
     */
    @Override
    public void removePassenger(PassengerRemoveReqDTO requestParam) {
        // 创建一个默认事务定义
        TransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        // 通过事务管理器获取一个事务状态
        TransactionStatus transactionStatus = transactionManager.getTransaction(transactionDefinition);

        // 获取当前用户名
        String username = UserContext.getUsername();
        // 查找乘车人
        PassengerDO passengerDO = selectPassenger(username, requestParam.getId());
        if (Objects.isNull(passengerDO)) {
            throw new ClientException("乘车人数据不存在");
        }
        try {
            // 构造修改条件
            LambdaUpdateWrapper<PassengerDO> deleteWrapper = Wrappers.lambdaUpdate(PassengerDO.class)
                    .eq(PassengerDO::getUsername, username)
                    .eq(PassengerDO::getId, requestParam.getId());
            // 逻辑删除，修改数据库表记录 del_flag
            int deleted = passengerMapper.delete(deleteWrapper);
            if (!SqlHelper.retBool(deleted)) {
                throw new ServiceException(String.format("[%s] 删除乘车人失败", username));
            }
            // 手动提交事务
            transactionManager.commit(transactionStatus);
        } catch (Exception ex) {
            if (ex instanceof ServiceException) {
                log.error("{}，请求参数：{}", ex.getMessage(), JSON.toJSONString(requestParam));
            } else {
                log.error("[{}] 删除乘车人失败，请求参数：{}", username, JSON.toJSONString(requestParam), ex);
            }
            // 手动回滚事务
            transactionManager.rollback(transactionStatus);
            throw ex;
        }
        // 删除缓存
        delUserPassengerCache(username);
    }

    /**
     * 根据用户名和用户id查找乘车人信息
     * @param username 用户名
     * @param passengerId 用户id
     * @return 乘车人信息
     */
    private PassengerDO selectPassenger(String username,String passengerId) {
        return passengerMapper.selectOne(Wrappers.lambdaQuery(PassengerDO.class)
                .eq(PassengerDO::getUsername,username)
                .eq(PassengerDO::getId,passengerId)
        );
    }

    /**
     * 删除乘车人信息缓存
     * @param username 用户名
     */
    private void delUserPassengerCache(String username){
        distributedCache.delete(USER_PASSENGER_LIST + username);
    }
}
