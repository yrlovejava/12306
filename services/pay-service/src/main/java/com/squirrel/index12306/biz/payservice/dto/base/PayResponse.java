package com.squirrel.index12306.biz.payservice.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付返回
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "支付返回")
public final class PayResponse {

    /**
     * 调用支付返回信息
     */
    @Schema(description = "调用支付返回信息")
    private String body;
}
