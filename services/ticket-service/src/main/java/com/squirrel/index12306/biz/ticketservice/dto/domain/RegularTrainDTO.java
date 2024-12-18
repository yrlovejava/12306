package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 普通车实体
 */
@Data
@Schema(description = "普通车实体")
public class RegularTrainDTO {

    /**
     * 软卧数量
     */
    @Schema(description = "软卧数量")
    private Integer softSleeperQuantity;

    /**
     * 软卧候选标识
     */
    @Schema(description = "软卧候选标识")
    private Boolean softSleeperCandidate;

    /**
     * 软卧价格
     */
    @Schema(description = "软卧价格")
    private Integer softSleeperPrice;

    /**
     * 高级软卧数量
     */
    @Schema(description = "高级软卧数量")
    private Integer deluxeSoftSleeperQuantity;

    /**
     * 高级软卧候选标识
     */
    @Schema(description = "高级软卧候选标识")
    private Boolean deluxeSoftSleeperCandidate;

    /**
     * 高级软卧价格
     */
    @Schema(description = "高级软卧价格")
    private Integer deluxeSoftSleeperPrice;

    /**
     * 硬卧数量
     */
    @Schema(description = "硬卧数量")
    private Integer hardSleeperQuantity;

    /**
     * 硬卧候选标识
     */
    @Schema(description = "硬卧候选标识")
    private Boolean hardSleeperCandidate;

    /**
     * 硬卧价格
     */
    @Schema(description = "硬卧价格")
    private Integer hardSleeperPrice;

    /**
     * 硬座数量
     */
    @Schema(description = "硬座数量")
    private Integer hardSeatQuantity;

    /**
     * 硬座候选标识
     */
    @Schema(description = "硬座候选标识")
    private Boolean hardSeatCandidate;

    /**
     * 硬座价格
     */
    @Schema(description = "硬座价格")
    private Integer hardSeatPrice;
}
