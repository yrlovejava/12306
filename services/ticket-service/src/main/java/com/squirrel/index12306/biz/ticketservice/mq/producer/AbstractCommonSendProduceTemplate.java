package com.squirrel.index12306.biz.ticketservice.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.messaging.Message;
import org.apache.rocketmq.spring.core.RocketMQTemplate;

import java.util.Optional;

/**
 * RocketMQ 抽象公共发送消息组件
 * @param <T> 泛型
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractCommonSendProduceTemplate<T> {

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 构建消息发送事件基础扩充属性实体
     * @param messageSendEvent 消息发送事件
     * @return 扩充属性实体
     */
    protected abstract BaseSendExtendDTO buildBaseSendExtendParam(T messageSendEvent);

    /**
     * 构建消息基本参数，请求头、Keys...
     * @param messageSendEvent 消息发送事件
     * @param requestParam 扩充属性实体
     * @return 消息基本参数
     */
    protected abstract Message<?> buildMessage(T messageSendEvent,BaseSendExtendDTO requestParam);

    /**
     * 消息事件通用发送
     * @param messageSendEvent 消息发送事件
     * @return 消息发送返回结果
     */
    public SendResult sendMessage(T messageSendEvent) {
        // 构建消息发送事件基础
        BaseSendExtendDTO baseSendExtendDTO = this.buildBaseSendExtendParam(messageSendEvent);
        SendResult sendResult;
        try {
            StringBuilder destinationBuilder = StrUtil.builder().append(baseSendExtendDTO.getTopic());
            if(StrUtil.isNotBlank(baseSendExtendDTO.getTag())) {
                destinationBuilder.append(":").append(baseSendExtendDTO.getTag());
            }
            // 同步发送消息
            sendResult = rocketMQTemplate.syncSend(
                    destinationBuilder.toString(),
                    this.buildMessage(messageSendEvent,baseSendExtendDTO),
                    baseSendExtendDTO.getSentTimeout(),
                    Optional.ofNullable(baseSendExtendDTO.getDelayLevel()).orElse(0));
            log.info("[{}] 消息发送结果：{}，消息ID：{}，消息Keys：{}", baseSendExtendDTO.getEventName(), sendResult.getSendStatus(), sendResult.getMsgId(), baseSendExtendDTO.getKeys());
        }catch (Throwable ex) {
            log.error("[{}] 消息发送失败，消息体：{}", baseSendExtendDTO.getEventName(), JSON.toJSONString(messageSendEvent), ex);
            throw ex;
        }
        return sendResult;
    }
}
