package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.service.BootstrapService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 启动引导接口
 */
@RestController
@RequestMapping("/api")
public class BootstrapController {

    private final BootstrapService bootstrapService;

    public BootstrapController(BootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    /** 一次性拉取全部业务数据 */
    @GetMapping("/bootstrap")
    public ApiResult<Map<String, Object>> bootstrap() {
        return ApiResult.ok(bootstrapService.load());
    }

    /** 健康检查，便于确认服务与数据库是否就绪 */
    @GetMapping("/health")
    public ApiResult<Map<String, Object>> health() {
        Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("status", "UP");
        info.put("service", "bbb-server");
        info.put("time", new java.util.Date().toString());
        return ApiResult.ok(info);
    }
}
