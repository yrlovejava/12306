package com.squirrel.index12306.biz.ticketservice.remote.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 车票订单创建请求参数
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "车票订单创建请求参数")
public class TicketOrderCreateRemoteReqDTO {

    /**
     * 用户 ID
     */
    @Schema(description = "用户ID")
    private String userId;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

    /**
     * 车次 ID
     */
    @Schema(description = "车次 ID")
    private Long trainId;

    /**
     * 出发站点
     */
    @Schema(description = "出发站点")
    private String departure;

    /**
     * 到达站点
     */
    @Schema(description = "到达站点")
    private String arrival;

    /**
     * 订单来源
     */
    @Schema(description = "订单来源")
    private Integer source;

    /**
     * 下单时间
     */
    @Schema(description = "下单时间")
    private Date orderTime;

    /**
     * 乘车日期
     */
    @Schema(description = "乘车日期")
    private Date ridingDate;

    /**
     * 列车车次
     */
    @Schema(description = "列车车次")
    private String trainNumber;

    /**
     * 出发时间
     */
    @Schema(description = "出发时间")
    private Date departureTime;

    /**
     * 到达时间
     */
    @Schema(description = "到达时间")
    private Date arrivalTime;

    /**
     * 订单明细
     */
    @Schema(description = "订单明细")
    private List<TicketOrderItemCreateRemoteReqDTO> ticketOrderItems;
}
