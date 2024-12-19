package com.squirrel.index12306.biz.ticketservice.dto.resp;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.squirrel.index12306.biz.ticketservice.dto.domain.TicketListDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 车票分页查询响应参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "车票分页查询返回参数")
public class TicketPageQueryRespDTO {

    /**
     * 车次集合数据
     */
    @Schema(description = "车次集合数据")
    private List<TicketListDTO> trainList;

    /**
     * 车次类型: D-动车 Z-直达 复兴号等
     */
    @Schema(description = "车次类型: D-动车 Z-直达 复兴号等")
    private List<String> trainBrandList;

    /**
     * 出发车站
     */
    @Schema(description = "出发车站")
    private List<String> departureStationList;

    /**
     * 到达车站
     */
    @Schema(description = "到达车站")
    private List<String> arrivalStationList;

    /**
     * 车次席别
     */
    @Schema(description = "车次席别")
    private List<String> seatClassList;
}
