package com.squirrel.index12306.biz.userservice.controller;

import com.squirrel.index12306.biz.userservice.dto.req.PassengerRemoveReqDTO;
import com.squirrel.index12306.biz.userservice.dto.req.PassengerReqDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.PassengerActualRespDTO;
import com.squirrel.index12306.biz.userservice.dto.resp.PassengerRespDTO;
import com.squirrel.index12306.biz.userservice.service.PassengerService;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import com.squirrel.index12306.framework.starter.idempotent.annotation.Idempotent;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentSceneEnum;
import com.squirrel.index12306.framework.starter.idempotent.enums.IdempotentTypeEnum;
import com.squirrel.index12306.framework.starter.web.Results;
import com.squirrel.index12306.frameworks.starter.user.core.UserContext;
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
     * @return Result<List<PassengerRespDTO>> 乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query")
    public Result<List<PassengerRespDTO>> listPassengerQueryByUsername() {
        return Results.success(passengerService.listPassengerQueryByUsername(UserContext.getUsername()));
    }

    /**
     * 新增乘车人
     * @param requestParam 新增乘车人的信息
     * @return Result<Void>
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-user:lock_passenger-alter:",
            key = "T(com.squirrel.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在新增乘车人，请稍后再试"
    )
    @PostMapping("/api/user-service/passenger/save")
    public Result<Void> savePassenger(@RequestBody PassengerReqDTO requestParam) {
        passengerService.savePassenger(requestParam);
        return Results.success();
    }

    /**
     * 修改乘车人
     * @param requestParam 修改的乘车人信息
     * @return Result<Void>
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-user:lock_passenger-alter:",
            key = "T(com.squirrel.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在修改乘车人，请稍后再试..."
    )
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
    @GetMapping("/api/user-service/inner/passenger/actual/query/ids")
    public Result<List<PassengerActualRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username, @RequestParam("ids")List<Long> ids) {
        return Results.success(passengerService.listPassengerQueryByIds(username, ids));
    }

    /**
     * 移除乘车人
     * @param requestParam 移除的乘车人信息
     * @return Result<Void>
     */
    @Idempotent(
            uniqueKeyPrefix = "index12306-user:lock_passenger-alter:",
            key = "T(com.squirrel.index12306.frameworks.starter.user.core.UserContext).getUsername()",
            type = IdempotentTypeEnum.SPEL,
            scene = IdempotentSceneEnum.RESTAPI,
            message = "正在移除乘车人，请稍后再试..."
    )
    @PostMapping("/api/user-service/passenger/remove")
    public Result<Void> removePassenger(@RequestBody PassengerRemoveReqDTO requestParam) {
        passengerService.removePassenger(requestParam);
        return Results.success();
    }
}
