package com.squirrel.index12306.biz.orderservice.dto.req;

import com.squirrel.index12306.framework.starter.convention.page.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 本人车票订单分页查询参数
 */
@Data
@Schema(description = "本人车票订单分页查询参数")
public class TicketOrderSelfPageQueryReqDTO extends PageRequest {

    /**
     * 证件号
     */
    @Schema(description = "证件号")
    private String idCard;
}
