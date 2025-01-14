package com.squirrel.index12306.biz.userservice.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.squirrel.index12306.biz.userservice.common.enums.UserChainMarkEnum;
import com.squirrel.index12306.biz.userservice.dao.entity.*;
import com.squirrel.index12306.biz.userservice.dao.mapper.*;
import com.squirrel.index12306.biz.userservice.dto.req.UserDeletionReqDTO;
import com.squirrel.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserLoginRespDTO;
import com.squirrel.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserRegisterRespDTO;
import com.squirrel.index12306.biz.userservice.service.UserLoginService;
import com.squirrel.index12306.biz.userservice.service.UserService;
import com.squirrel.index12306.biz.userservice.toolkit.UserReuseUtil;
import com.squirrel.index12306.framework.starter.cache.DistributedCache;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import com.squirrel.index12306.framework.starter.convention.exception.ServiceException;
import com.squirrel.index12306.framework.starter.designpattern.chain.AbstractChainContext;
import com.squirrel.index12306.frameworks.starter.user.core.UserContext;
import com.squirrel.index12306.frameworks.starter.user.core.UserInfoDTO;
import com.squirrel.index12306.frameworks.starter.user.toolkit.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.squirrel.index12306.biz.userservice.common.constant.RedisKeyConstant.USER_DELETION;
import static com.squirrel.index12306.biz.userservice.common.constant.RedisKeyConstant.USER_REGISTER_REUSE_SHARDING;
import static com.squirrel.index12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum.*;

/**
 * 用户登录接口实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final UserDeletionMapper userDeletionMapper;
    private final UserReuseMapper userReuseMapper;
    private final UserMailMapper userMailMapper;
    private final UserPhoneMapper userPhoneMapper;
    private final DistributedCache distributedCache;
    private final AbstractChainContext<UserRegisterReqDTO> abstractChainContext;
    private final RedissonClient redissonClient;
    private final RBloomFilter<String> userRegisterCachePenetrationBloomFilter;

    /**
     * 登录操作
     *
     * @param requestParam 用户登录
     * @return 用户登录返回结果
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO requestParam) {
        String usernameOrMailOrPhone = requestParam.getUsernameOrMailOrPhone();
        boolean mailFlag = false;
        // 判断是否是邮箱登录
        // 时间复杂度最佳O（1）。indexOf or contains 时间复杂度为O（n）
        for (char c : usernameOrMailOrPhone.toCharArray()){
            if(c == '@'){
                mailFlag = true;
                break;
            }
        }
        String username;
        if(mailFlag){
            // 根据邮箱在数据库中查找用户信息
            LambdaQueryWrapper<UserMailDO> queryWrapper = Wrappers.lambdaQuery(UserMailDO.class)
                    .eq(UserMailDO::getMail, usernameOrMailOrPhone);
            username = Optional.ofNullable(userMailMapper.selectOne(queryWrapper))
                    .map(UserMailDO::getUsername)
                    .orElseThrow(() -> new ClientException("用户名/手机号/邮箱不存在"));
        }else {
            // 根据手机号在数据库中查找用户信息
            LambdaQueryWrapper<UserPhoneDO> queryWrapper = Wrappers.lambdaQuery(UserPhoneDO.class)
                    .eq(UserPhoneDO::getPhone, usernameOrMailOrPhone);
            username = Optional.ofNullable(userPhoneMapper.selectOne(queryWrapper))
                    .map(UserPhoneDO::getUsername)
                    .orElse(null);
        }
        // 如果到这里username还是null，证明是用户名登录
        username = Optional.ofNullable(username).orElse(requestParam.getUsernameOrMailOrPhone());

        // 构建查询条件
        LambdaQueryWrapper<UserDO> queryWrapper = Wrappers.<UserDO>lambdaQuery()
                .eq(UserDO::getUsername, username)
                .eq(UserDO::getPassword, requestParam.getPassword())
                .select(UserDO::getId,UserDO::getUsername,UserDO::getRealName);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        if (userDO != null) {
            UserInfoDTO userInfo = UserInfoDTO.builder()
                    .userId(String.valueOf(userDO.getId()))
                    .username(userDO.getUsername())
                    .realName(userDO.getRealName())
                    .build();
            String accessToken = JWTUtil.generateAccessToken(userInfo);
            UserLoginRespDTO actual = new UserLoginRespDTO(
                    userInfo.getUserId(),
                    requestParam.getUsernameOrMailOrPhone(),
                    userDO.getRealName(),
                    accessToken);
            distributedCache.put(accessToken, JSON.toJSONString(actual), 30, TimeUnit.MINUTES);
            return actual;
        }
        throw new ServiceException("账号不存在或密码错误");
    }

    /**
     * 验证登录
     *
     * @param accessToken 用户登录 Token 凭证
     * @return 验证登录返回结果
     */
    @Override
    public UserLoginRespDTO checkLogin(String accessToken) {
        return distributedCache.get(accessToken, UserLoginRespDTO.class);
    }

    /**
     * 登出操作
     *
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
     *
     * @param username 用户名
     * @return 是否存在
     */
    @Override
    public Boolean hasUsername(String username) {
        // 查询布隆过滤器是否存在
        boolean hasUsername = userRegisterCachePenetrationBloomFilter.contains(username);
        if (hasUsername) {
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            // 在可复用的用户名集合中去查询是否可用，如果存在就是可用
            return instance.opsForSet().isMember(USER_REGISTER_REUSE_SHARDING + UserReuseUtil.hashShardingIdx(username), username);
        }
        return true;
    }

    /**
     * 用户注册
     *
     * @param requestParam 用户注册入参
     * @return 注册返回结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserRegisterRespDTO register(UserRegisterReqDTO requestParam) {
        abstractChainContext.handler(UserChainMarkEnum.USER_REGISTER_FILTER.name(), requestParam);
        try {
            int inserted = userMapper.insert(BeanUtil.convert(requestParam, UserDO.class));
            if (inserted < 1) {
                throw new ServiceException(USER_REGISTER_FAIL);
            }
        } catch (DuplicateKeyException dke) {
            log.error("用户名 [{}] 重复注册", requestParam.getUsername());
            throw new ServiceException(HAS_USERNAME_NOTNULL);
        }

        // 在手机号表中插入数据
        UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                .phone(requestParam.getPhone())
                .username(requestParam.getUsername())
                .build();
        try {
            userPhoneMapper.insert(userPhoneDO);
        }catch (DuplicateKeyException dke) {
            log.error("用户 [{}] 注册手机号 [{}] 重复", requestParam.getUsername(), requestParam.getPhone());
            throw new ServiceException(PHONE_REGISTERED);
        }

        // 如果邮箱字段不为空，在邮箱表中插入数据
        if (StrUtil.isNotBlank(requestParam.getMail())) {
            UserMailDO userMailDO = UserMailDO.builder()
                    .mail(requestParam.getMail())
                    .username(requestParam.getUsername())
                    .build();
            try {
                userMailMapper.insert(userMailDO);
            } catch (DuplicateKeyException dke) {
                log.error("用户 [{}] 注册邮箱 [{}] 重复", requestParam.getUsername(), requestParam.getMail());
                throw new ServiceException(MAIL_REGISTERED);
            }
        }

        // 先删数据库，再删缓存
        // 获取用户名
        String username = requestParam.getUsername();
        // 删除数据库中可复用的用户名数据
        userReuseMapper.delete(Wrappers.update(
                UserReuseDO.builder()
                        .username(username)
                        .build())
        );

        // 在可复用的用户名的集合中删除
        StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
        instance.opsForSet().remove(USER_REGISTER_REUSE_SHARDING + UserReuseUtil.hashShardingIdx(username), username);

        // 在布隆过滤器中添加
        userRegisterCachePenetrationBloomFilter.add(username);
        return BeanUtil.convert(requestParam, UserRegisterRespDTO.class);
    }

    /**
     * 注销用户
     *
     * @param requestParam 注销用户入参
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletion(UserDeletionReqDTO requestParam) {
        String username = UserContext.getUsername();
        if (!Objects.equals(username, requestParam.getUsername())) {
            // 如果用户名跟当前登录用户不一致
            // 此处严谨来说，需要上报风控中心进行异常检测
            throw new ClientException("注销账号与登录账号不一致");
        }
        RLock lock = redissonClient.getLock(USER_DELETION + requestParam.getUsername());
        // lock()放try-catch块外面，如果异常将不会执行unlock，如果放在try-catch块中，如果lock()抛出异常，仍然会执行unlock(),这里出现问题
        lock.lock();
        try {
            // 查询用户信息
            UserQueryRespDTO userQueryRespDTO = userService.queryUserByUsername(username);
            UserDeletionDO userDeletionDO = UserDeletionDO.builder()
                    .idType(userQueryRespDTO.getIdType())
                    .idCard(userQueryRespDTO.getIdCard())
                    .build();
            // 插入注销记录
            userDeletionMapper.insert(userDeletionDO);

            // MyBatis Plus 不支持修改语句变更 del_flag 字段
            // 逻辑删除用户数据
            UserDO userDO = new UserDO();
            userDO.setDeletionTime(System.currentTimeMillis());
            userDO.setUsername(username);
            userMapper.deletionUser(userDO);
            // 逻辑删除用户手机号数据
            UserPhoneDO userPhoneDO = UserPhoneDO.builder()
                    .phone(userQueryRespDTO.getPhone())
                    .deletionTime(System.currentTimeMillis())
                    .build();
            userPhoneMapper.deletionUser(userPhoneDO);
            // 逻辑删除用户邮箱数据
            if (StrUtil.isNotBlank(userQueryRespDTO.getMail())) {
                UserMailDO userMailDO = UserMailDO.builder()
                        .mail(userQueryRespDTO.getMail())
                        .deletionTime(System.currentTimeMillis())
                        .build();
                userMailMapper.deletionUser(userMailDO);
            }
            // 删除缓存中用户的token
            distributedCache.delete(UserContext.getToken());

            // 恢复数据库中可复用的用户名
            userReuseMapper.insert(
                    UserReuseDO.builder()
                    .username(username)
                    .build());
            // 恢复redis中可复用用户名缓存
            StringRedisTemplate instance = (StringRedisTemplate) distributedCache.getInstance();
            instance.opsForSet().add(USER_REGISTER_REUSE_SHARDING + UserReuseUtil.hashShardingIdx(username), username);
        } finally {
            lock.unlock();
        }
    }
}
