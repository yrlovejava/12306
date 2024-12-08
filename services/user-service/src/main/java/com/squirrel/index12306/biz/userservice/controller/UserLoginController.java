package com.squirrel.index12306.biz.userservice.controller;

import com.squirrel.index12306.biz.userservice.dto.req.UserLoginReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.UserLoginRespDTO;
import com.squirrel.index12306.biz.userservice.service.UserLoginService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户登录控制层
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "用户登录控制层")
public class UserLoginController {

    private final UserLoginService userLoginService;

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录")
    @PostMapping("/api/user-service/v1/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam) {
        return Results.success(userLoginService.login(requestParam));
    }

    /**
     * 通过 Token 检查用户是否登录
     */
    @Operation(summary = "通过 Token 检查用户是否登录")
    @GetMapping("/api/user-service/check-login")
    public Result<UserLoginRespDTO> checkLogin(@RequestParam("accessToken") String accessToken) {
        UserLoginRespDTO result = userLoginService.checkLogin(accessToken);
        return Results.success(result);
    }

    /**
     * 用户退出登录
     */
    @Operation(summary = "用户退出登录")
    @GetMapping("/api/user-service/logout")
    public Result<Void> logout(@RequestParam(required = false) String accessToken) {
        userLoginService.logout(accessToken);
        return Results.success();
    }
}
