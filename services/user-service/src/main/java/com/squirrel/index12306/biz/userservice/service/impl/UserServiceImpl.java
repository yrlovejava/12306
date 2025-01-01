package com.squirrel.index12306.biz.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.userservice.dao.entity.UserDO;
import com.squirrel.index12306.biz.userservice.dao.entity.UserDeletionDO;
import com.squirrel.index12306.biz.userservice.dao.entity.UserMailDO;
import com.squirrel.index12306.biz.userservice.dao.mapper.UserDeletionMapper;
import com.squirrel.index12306.biz.userservice.dao.mapper.UserMailMapper;
import com.squirrel.index12306.biz.userservice.dao.mapper.UserMapper;
import com.squirrel.index12306.biz.userservice.dto.req.UserUpdateReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import com.squirrel.index12306.biz.userservice.service.UserService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * 用户信息接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserDeletionMapper userDeletionMapper;
    private final UserMailMapper userMailMapper;

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param userId 用户 ID
     * @return 用户详细信息
     */
    @Override
    public UserQueryRespDTO queryUserByUserId(String userId) {
        // 根据id查询数据库中用户信息
        UserDO userDO = userMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getId, userId));
        if(userDO == null){
            throw new ClientException("用户不存在，请检查用户ID是否正确");
        }
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    @Override
    public UserQueryRespDTO queryUserByUsername(String username) {
        UserDO userDO = userMapper.selectOne(Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username));
        if (userDO == null) {
            throw new ClientException("用户不存在，请检查用户名是否正确");
        }
        return BeanUtil.convert(userDO, UserQueryRespDTO.class);
    }

    /**
     * 根据证件类型和证件号查询注销次数
     *
     * @param idType 证件类型
     * @param idCard 证件号
     * @return 注销次数
     */
    @Override
    public Integer queryUserDeletionNum(Integer idType, String idCard) {
        // TODO 此处应该先查缓存
        // 查询数据库中注销的记录数量
        Long deletionCount = userDeletionMapper.selectCount(Wrappers.lambdaQuery(UserDeletionDO.class)
                .eq(UserDeletionDO::getIdType, idType)
                .eq(UserDeletionDO::getIdCard, idCard));
        return Optional.ofNullable(deletionCount)
                .map(Long::intValue)
                .orElse(0);
    }

    /**
     * 根据用户 ID 修改用户信息
     *
     * @param requestParam 用户信息入参
     */
    @Override
    public void update(UserUpdateReqDTO requestParam) {
        // 根据用户名查询用户信息
        UserQueryRespDTO userQueryRespDTO = this.queryUserByUsername(requestParam.getUsername());
        // 转换为userDO类型
        UserDO userDO = BeanUtil.convert(requestParam, UserDO.class);
        // 修改数据库中数据
        LambdaUpdateWrapper<UserDO> userUpdateWrapper = Wrappers.lambdaUpdate(UserDO.class)
                .eq(UserDO::getUsername, requestParam.getUsername());
        userMapper.update(userDO, userUpdateWrapper);
        // 如果mail需要修改
        if(StrUtil.isNotBlank(requestParam.getMail()) && !Objects.equals(requestParam.getMail(),userQueryRespDTO.getMail())){
            // 先删除邮箱表中的数据，再新增数据
            LambdaUpdateWrapper<UserMailDO> updateWrapper = Wrappers.lambdaUpdate(UserMailDO.class)
                    .eq(UserMailDO::getMail, userQueryRespDTO.getMail());
            userMailMapper.delete(updateWrapper);
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            userMailMapper.insert(userMailDO);
        }
    }
}
