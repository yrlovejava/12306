package com.squirrel.index12306.biz.userservice.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.userservice.dao.entity.UserDO;
import com.squirrel.index12306.biz.userservice.dao.mapper.UserMapper;
import com.squirrel.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import com.squirrel.index12306.biz.userservice.service.UserService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户信息接口实现层
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final DistributedCache distributedCache;

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
}
