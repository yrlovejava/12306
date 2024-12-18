package com.squirrel.index12306.biz.ticketservice.dto.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 高铁列车实体
 */
@Data
@Schema(description = "高铁列车实体")
public class BulletTrainDTO {

    /**
     * 动卧数量
     */
    @Schema(description = "动卧数量")
    private Integer sleeperQuantity;

    /**
     * 动卧候选标识
     */
    @Schema(description = "动卧候选标识")
    private Boolean sleeperCandidate;

    /**
     * 动卧价格
     */
    @Schema(description = "动卧价格")
    private Integer sleeperPrice;

    /**
     * 一等卧数量
     */
    @Schema(description = "一等卧数量")
    private Integer firstSleeperQuantity;

    /**
     * 一等卧候选标识
     */
    @Schema(description = "一等卧候选标识")
    private Boolean firstSleeperCandidate;

    /**
     * 一等卧价格
     */
    @Schema(description = "一等卧价格")
    private Integer firstSleeperPrice;

    /**
     * 二等卧数量
     */
    @Schema(description = "二等卧数量")
    private Integer secondSleeperQuantity;

    /**
     * 二等卧候选标识
     */
    @Schema(description = "二等卧候选标识")
    private Boolean secondSleeperCandidate;

    /**
     * 二等卧价格
     */
    @Schema(description = "二等卧价格")
    private Integer secondSleeperPrice;

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
