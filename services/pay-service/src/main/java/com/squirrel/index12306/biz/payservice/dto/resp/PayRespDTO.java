package com.squirrel.index12306.biz.payservice.dto.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 支付返回实体
 */
@Data
@Schema(description = "支付返回实体")
public class PayRespDTO {

    /**
     * 调用支付返回信息
     */
    @Schema(description = "调用支付返回信息")
    private String body;
}
