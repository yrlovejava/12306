package com.squirrel.index12306.biz.orderservice.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 车票订单详情创建请求参数
 */
@Data
@Schema(description = "车票订饭详情创建请求参数")
public class TicketOrderItemCreateReqDTO {

    /**
     * 车厢号
     */
    @Schema(description = "车厢号")
    private String carriageNumber;

    /**
     * 座位号
     */
    @Schema(description = "座位号")
    private String seatNumber;

    /**
     * 真实姓名
     */
    @Schema(description = "真实姓名")
    private String realName;

    /**
     * 证件类型
     */
    @Schema(description = "证件类型")
    private Integer idType;

    /**
     * 证件号
     */
    @Schema(description = "证件号")
    private String idCard;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 订单金额
     */
    @Schema(description = "订单金额")
    private Integer amount;
}
