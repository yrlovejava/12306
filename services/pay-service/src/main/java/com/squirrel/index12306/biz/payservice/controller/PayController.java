package com.squirrel.index12306.biz.payservice.controller;

import com.squirrel.index12306.biz.payservice.convert.PayRequestConvert;
import com.squirrel.index12306.biz.payservice.dto.PayCommand;
import com.squirrel.index12306.biz.payservice.dto.PayInfoRespDTO;
import com.squirrel.index12306.biz.payservice.dto.PayRespDTO;
import com.squirrel.index12306.biz.payservice.dto.base.PayRequest;
import com.squirrel.index12306.biz.payservice.service.PayService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 支付控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "支付控制层")
public class PayController {

    private final PayService payService;

    /**
     * 公共支付接口
     * 对接常用支付方式，比如：支付宝、微信以及银行卡等
     * @param requestParam 支付参数
     * @return Result<PayRespDTO>
     */
    @Operation(summary = "公共支付接口")
    @PostMapping("/api/pay-service/create/pay")
    public Result<PayRespDTO> pay(@RequestBody PayCommand requestParam) {
        PayRequest payRequest = PayRequestConvert.command2PayRequest(requestParam);
        PayRespDTO result = payService.commonPay(payRequest);
        return Results.success(result);
    }

    /**
     * 查询支付结果接口
     * @param orderSn 订单号
     * @return Result<PayInfoRespDTO>
     */
    @Operation(summary = "查询支付结果接口")
    @GetMapping("/api/pay-service/get/pay/info")
    public Result<PayInfoRespDTO> getPayInfo(@RequestParam(value = "orderSn") String orderSn) {
        return Results.success(payService.getPayInfo(orderSn));
    }
}
