package com.squirrel.index12306.biz.ticketservice.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.squirrel.index12306.framework.starter.convention.page.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

/**
 * 车票分页查询请求参数
 */
@Data
@Schema(description = "车票分页查询请求参数")
public class TicketPageQueryReqDTO extends PageRequest {

    /**
     * 出发地 Code(地区编码)
     */
    @Schema(description = "出发地")
    private String fromStation;

    /**
     * 目的地 Code(地区编码)
     */
    @Schema(description = "目的地")
    private String toStation;

    /**
     * 出发日期
     */
    @Schema(description = "出发日期")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date departureDate;

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
}
