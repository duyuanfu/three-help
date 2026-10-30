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

    /** 微信登录：小程序端 uni.login 拿到的 code 换登录态；未绑定手机号时返回 needBind=true + ticket */
    @PostMapping("/wx-login")
    public ApiResult<Map<String, Object>> wxLogin(@RequestBody AuthService.WxLoginRequest req) {
        return ApiResult.ok(authService.wxLogin(req));
    }

    /** 微信登录后绑定手机号：首次即注册，老账号需校验密码 */
    @PostMapping("/wx-bind")
    public ApiResult<Map<String, Object>> wxBind(@RequestBody AuthService.WxBindRequest req) {
        return ApiResult.ok(authService.wxBind(req));
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
