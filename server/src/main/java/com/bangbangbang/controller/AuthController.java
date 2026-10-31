package com.bangbangbang.controller;

import com.bangbangbang.common.ApiResult;
import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 账号接口：注册 / 登录 / 退出 / 当前用户
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResult<Map<String, Object>> register(@RequestBody AuthService.RegisterRequest req) {
        return ApiResult.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResult<Map<String, Object>> login(@RequestBody AuthService.LoginRequest req) {
        return ApiResult.ok(authService.login(req));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7).trim());
        }
        return ApiResult.ok();
    }

    @GetMapping("/me")
    public ApiResult<SysUser> me() {
        return ApiResult.ok(authService.getById(UserContext.require().id()));
    }
}
