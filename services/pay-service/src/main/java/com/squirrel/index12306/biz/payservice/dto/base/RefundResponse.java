package com.squirrel.index12306.biz.payservice.dto.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public final class RefundResponse {

    /**
     * 支付状态
     */
    private Integer status;
}
