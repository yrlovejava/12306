package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 席别类型实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "席别类型实体")
public class SeatClassDTO {

    /**
     * 席别类型
     */
    @Schema(description = "席别类型")
    private Integer type;

    /**
     * 席别数量
     */
    @Schema(description = "席别数量")
    private Integer quantity;

    /**
     * 席别价格
     */
    @Schema(description = "席别价格")
    private BigDecimal price;

    /**
     * 席别候补标识
     */
    @Schema(description = "席别候补标识")
    private Boolean candidate;
}
