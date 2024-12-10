package com.squirrel.index12306.biz.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.userservice.dao.entity.UserDO;
import com.squirrel.index12306.biz.userservice.dao.mapper.UserMapper;
import com.squirrel.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserLoginRespDTO;
import com.squirrel.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserRegisterRespDTO;
import com.squirrel.index12306.biz.userservice.service.UserLoginService;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.frameworks.starter.user.core.UserInfoDTO;
import com.squirrel.index12306.frameworks.starter.user.toolkit.JWTUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 用户登录接口实现
 */
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final UserMapper userMapper;
    private final DistributedCache distributedCache;

    /**
     * 登录操作
     * @param requestParam 用户登录
     * @return 用户登录返回结果
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        // 1.构建查询条件
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, requestParam.getUsernameOrMailOrPhone())
                .eq(UserDO::getPassword, requestParam.getPassword());
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO != null) {
            UserInfoDTO userInfo = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))
                    .username(userDO.getUsername())
                    .realName(userDO.getRealName())
                    .build();
            String accessToken = JWTUtil.generateAccessToken(userInfo);
            UserLoginRespDTO actual = new UserLoginRespDTO(requestParam.getUsernameOrMailOrPhone(), userDO.getRealName(), accessToken);
            distributedCache.put(accessToken, JSON.toJSONString(actual), 30, TimeUnit.MINUTES);
            return actual;
        }
        throw new ServiceException("用户名不存在或密码错误");
    }

    /**
     * 验证登录
     * @param accessToken 用户登录 Token 凭证
     * @return 验证登录返回结果
     */
    @Override
    public UserLoginRespDTO checkLogin(String accessToken) {
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    /**
     * 登出操作
     * @param accessToken 用户登录 Token 凭证
     */
    @Override
    public void logout(String accessToken) {
        if (StrUtil.isNotBlank(accessToken)) {
            distributedCache.delete(accessToken);
        }
    }

    /**
     * 查询是否存在用户名
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public Boolean hasUsername(String username) {
        // TODO 需要使用布隆过滤器防止缓存穿透
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.lambdaQuery(UserDO.class)
                .eq(UserDO::getUsername, username);
        return userMapper.selectOne(queryWrapper) == null ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * 用户注册
     * @param requestParam 用户注册入参
     * @return 注册返回结果
     */
    @Override
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        // TODO 责任链模式校验用户名，身份证、手机号格式等
        if (!hasUsername(requestParam.getUsername())) {
            throw new ClientException("用户名已存在");
        }
        int inserted = userMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
        if (inserted < 1) {
            throw new ServiceException("用户注册失败");
        }
        return BeanUtil.convert(requestParam, UserRegisterRespDTO.class);
    }
}
