package com.squirrel.index12306.biz.payservice.mq.domain;

import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

/**
 * 消息体包装器
 * @param <T>
 */
@Data
@Builder
@NoArgsConstructor(force = true)
@AllArgsConstructor
@RequiredArgsConstructor
public class MessageWrapper<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息发送 Keys
     */
    @NonNull
    private String keys;

    /**
     * 消息体
     */
    @NonNull
    private T message;

    /**
     * 唯一标识，用于客户端幂等验证
     */
    private String uuid = UUID.randomUUID().toString();

    /**
     * 消息发送时间
     */
    private Long timestamp = System.currentTimeMillis();
}