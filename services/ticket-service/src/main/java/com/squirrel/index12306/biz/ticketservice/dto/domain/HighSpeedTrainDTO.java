package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 动车实体
 */
@Data
@Schema(description = "动车实体")
public class HighSpeedTrainDTO {

    /**
     * 商务座数量
     */
    @Schema(description = "商务座数量")
    private Integer businessClassQuantity;

    /**
     * 商务座候选标识
     */
    @Schema(description = "商务座候选标识")
    private Boolean businessClassCandidate;

    /**
     * 商务座价格
     */
    @Schema(description = "商务座价格")
    private Integer businessClassPrice;

    /**
     * 一等座数量
     */
    @Schema(description = "一等座数量")
    private Integer firstClassQuantity;

    /**
     * 一等座候选标识
     */
    @Schema(description = "一等座候选标识")
    private Boolean firstClassCandidate;

    /**
     * 一等座价格
     */
    @Schema(description = "一等座价格")
    private Integer firstClassPrice;

    /**
     * 二等座数量
     */
    @Schema(description = "二等座数量")
    private Integer secondClassQuantity;

    /**
     * 二等座候选标识
     */
    @Schema(description = "二等座候选标识")
    private Boolean secondClassCandidate;

    /**
     * 二等座价格
     */
    @Schema(description = "二等座价格")
    private Integer secondClassPrice;
}
