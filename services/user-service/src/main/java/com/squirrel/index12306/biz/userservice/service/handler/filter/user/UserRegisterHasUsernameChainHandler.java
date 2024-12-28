package com.squirrel.index12306.biz.userservice.service.handler.filter.user;

import com.squirrel.index12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.squirrel.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.squirrel.index12306.biz.userservice.service.UserLoginService;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 用户注册用户名唯一检验
 */
@Component
@RequiredArgsConstructor
public final class UserRegisterHasUsernameChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    private final UserLoginService userLoginService;

    /**
     * 执行责任链
     * @param requestParam 责任链执行入参
     */
    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        if(!userLoginService.hasUsername(requestParam.getUsername())){
            throw new ClientException(UserRegisterErrorCodeEnum.HAS_USERNAME_NOTNULL);
        }
    }

    /**
     * 设置优先级
     * @return 优先级
     */
    @Override
    public int getOrder() {
        return 1;
    }
}
