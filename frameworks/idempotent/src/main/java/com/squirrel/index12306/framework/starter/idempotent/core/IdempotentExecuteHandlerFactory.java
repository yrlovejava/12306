package com.squirrel.index12306.framework.starter.idempotent.core;

import com.squirrel.index12306.framework.starter.bases.ApplicationContextHolder;
import com.squirrel.index12306.framework.starter.idempotent.core.param.IdempotentParamService;
import com.squirrel.index12306.framework.starter.idempotent.core.spel.IdempotentSpEByRestAPIExecuteHandler;
import com.squirrel.index12306.framework.starter.idempotent.core.spel.IdempotentSpELByMQExecuteHandler;
import com.squirrel.index12306.framework.starter.idempotent.core.token.IdempotentTokenService;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;

/**
 * 幂等执行处理器工厂
 * <p>
 * 为什么要采用简单工厂模式？策略模式不行吗？
 * 策略模式同样可以达到真正幂等处理器功能，但是简单工厂的语意更适合这个场景，所以选择了简单工厂
 */
public final class IdempotentExecuteHandlerFactory {

    /**
     * 获取幂等执行处理器
     *
     * @param scene 指定幂等验证场景类型
     * @param type  指定幂等处理类型
     * @return 幂等执行处理器
     */
    public static IdempotentExecuteHandler getInstance(IdempotentSceneEnum scene, IdempotentTypeEnum type) {
        IdempotentExecuteHandler result = null;
        switch (scene) {
            case RESTAPI -> {
                switch (type) {
                    case PARAM -> result = ApplicationContextHolder.getBean(IdempotentParamService.class);
                    case TOKEN -> result = ApplicationContextHolder.getBean(IdempotentTokenService.class);
                    case SPEL -> result = ApplicationContextHolder.getBean(
                            IdempotentSpEByRestAPIExecuteHandler.class);
                    default -> {
                    }
                }
            }
            case MQ -> result = ApplicationContextHolder.getBean(IdempotentSpELByMQExecuteHandler.class);
            default -> {
            }
        }
        return result;
    }
}
