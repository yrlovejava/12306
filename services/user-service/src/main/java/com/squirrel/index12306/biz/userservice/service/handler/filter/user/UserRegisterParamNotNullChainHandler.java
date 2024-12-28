package com.squirrel.index12306.biz.userservice.service.handler.filter.user;

import com.squirrel.index12306.biz.userservice.common.enums.UserRegisterErrorCodeEnum;
import com.squirrel.index12306.biz.userservice.dto.req.UserRegisterReqDTO;
import com.squirrel.index12306.framework.starter.convention.exception.ClientException;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * 用户注册参数必填检验
 */
@Component
public final class UserRegisterParamNotNullChainHandler implements UserRegisterCreateChainFilter<UserRegisterReqDTO> {

    @Override
    public void handler(UserRegisterReqDTO requestParam) {
        if (Objects.isNull(requestParam.getUsername())) {
            throw new ClientException(UserRegisterErrorCodeEnum.USER_NAME_NOTNULL);
        } else if (Objects.isNull(requestParam.getPassword())) {
            throw new ClientException(UserRegisterErrorCodeEnum.PASSWORD_NOTNULL);
        } else if (Objects.isNull(requestParam.getPhone())) {
            throw new ClientException(UserRegisterErrorCodeEnum.PHONE_NOTNULL);
        } else if (Objects.isNull(requestParam.getIdType())) {
            throw new ClientException(UserRegisterErrorCodeEnum.ID_TYPE_NOTNULL);
        } else if (Objects.isNull(requestParam.getIdCard())) {
            throw new ClientException(UserRegisterErrorCodeEnum.ID_CARD_NOTNULL);
        }
        // xxx 这里应该把所有用户注册入参校验必填，因为重复工作量所以暂时验证上述这些
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
