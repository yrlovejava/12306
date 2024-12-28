package com.squirrel.index12306.biz.userservice.service.handler.filter.user;

import com.squirrel.index12306.biz.userservice.common.enums.UserChainMarkEnum;
import com.squirrel.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.squirrel.index12306.framework.starter.designpattern.chain.AbstractChainHandler;

/**
 * 用户注册责任链过滤器
 * @param <T> 泛型
 */
public interface UserRegisterCreateChainFilter<T extends UserRegisterReqDTO> extends AbstractChainHandler<UserRegisterReqDTO> {

    @Override
    default String mark(){
        return UserChainMarkEnum.USER_REGISTER_FILTER.name();
    }
}
