package com.squirrel.index12306.biz.orderservice.dto.req;

import com.squirrel.index12306.framework.starter.convention.page.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车票订单分页查询
 */
@Data
@Schema(description = "车票订单分页查询参数")
public class TicketOrderPageQueryReqDTO extends PageRequest {

    /**
     * 用户唯一标识
     */
    @Schema(description = "用户唯一标识")
    private String userId;

    /**
     * 状态类型 0：未完成 1：未出行 2：历史订单
     */
    @Schema(description = "状态类型")
    private Integer statusType;
}
