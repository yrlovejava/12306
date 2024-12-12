package com.squirrel.index12306.biz.userservice.controller;

import com.squirrel.index12306.biz.userservice.dto.req.PassengerReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import com.squirrel.index12306.biz.userservice.service.PassengerService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 乘车人控制层
 */
@RestController
@RequiredArgsConstructor
public class PassengerController {

    private final PassengerService passengerService;

    /**
     * 根据用户名查询乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername(String username) {
        return Results.success(passengerService.listPassengerQueryByUsername(username));
    }

    /**
     * 新增乘车人
     */
    @PostMapping("/api/user-service/passenger/save")
    public Result<Void> savePassenger(@RequestBody PassengerReqDTO requestParam) {
        passengerService.savePassenger(requestParam);
        return Results.success();
    }

    /**
     * 修改乘车人
     */
    @PostMapping("/api/user-service/passenger/update")
    public Result<Void> updatePassenger(@RequestBody PassengerReqDTO requestParam) {
        passengerService.updatePassenger(requestParam);
        return Results.success();
    }

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     * @param username 用户名
     * @param ids id集合
     * @return 乘车人列表
     */
    public Result<List<PassengerRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username, @RequestParam("ids")List<Long> ids) {
        return Results.success(passengerService.listPassengerQueryByIds(username, ids));
    }
}
