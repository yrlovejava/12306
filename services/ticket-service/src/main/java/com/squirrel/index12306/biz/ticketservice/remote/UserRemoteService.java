package com.squirrel.index12306.biz.ticketservice.remote;

import com.squirrel.index12306.biz.ticketservice.remote.dto.PassengerRespDTO;
import com.squirrel.index12306.framework.starter.convention.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 用户远程服务调用
 */
@FeignClient("index12306-user-service")
public interface UserRemoteService {

    /**
     * 根据乘车人 ID 集合查询乘车人列表
     */
    @GetMapping("/api/user-service/passenger/query/ids")
    Result<List<PassengerRespDTO>> listPassengerQueryByIds(@RequestParam("username") String username, @RequestParam("ids") List<String> ids);
}
