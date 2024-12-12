package com.squirrel.index12306.biz.ticketservice.remote.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * 乘车人返回参数
 */
@Data
@Schema(description = "乘车人返回参数")
public class PassengerRespDTO {

    /**
     * 乘车人id
     */
    @Schema(description = "乘车人id")
    private String id;

    /**
     * 用户名
     */
    @Schema(description = "用户名")
    private String username;

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
     * 证件号码
     */
    @Schema(example = "证件号码")
    private String idCard;

    /**
     * 优惠类型
     */
    @Schema(description = "优惠类型")
    private Integer discountType;

    /**
     * 手机号
     */
    @Schema(description = "手机号")
    private String phone;

    /**
     * 添加日期
     */
    @Schema(description = "添加日期")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private Date createDate;

    /**
     * 审核状态
     */
    @Schema(description = "审核状态")
    private Integer verifyStatus;
}
