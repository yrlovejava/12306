package com.squirrel.index12306.biz.userservice.service;

import com.squirrel.index12306.biz.userservice.dto.resp.UserQueryRespDTO;
import jakarta.validation.constraints.NotEmpty;

/**
 * 用户信息接口层
 */
public interface UserService {

    /**
     * 根据用户名查询用户信息
     *
     * @param username 用户名
     * @return 用户详细信息
     */
    UserQueryRespDTO queryUserByUsername(@NotEmpty String username);
}
