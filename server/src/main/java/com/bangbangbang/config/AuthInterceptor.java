package com.bangbangbang.config;

import com.bangbangbang.common.UserContext;
import com.bangbangbang.entity.SysUser;
import com.bangbangbang.mapper.SysUserMapper;
import com.bangbangbang.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * 认证拦截器：解析 Authorization: Bearer <token>，把当前用户写入 UserContext
 *
 * 未登录时直接返回 401 的 JSON（而不是跳转页面），由前端 api 层统一处理。
 */
@Slf4j
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final SysUserMapper sysUserMapper;

    public AuthInterceptor(TokenService tokenService, SysUserMapper sysUserMapper) {
        this.tokenService = tokenService;
        this.sysUserMapper = sysUserMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = extractToken(request);
        String userId = tokenService.resolveUserId(token);
        if (userId == null) {
            writeUnauthorized(response);
            return false;
        }

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            writeUnauthorized(response);
            return false;
        }

        UserContext.set(new UserContext.CurrentUser(
                user.getId(),
                user.getName(),
                user.getAvatar(),
                user.getPhone(),
                user.getIdentityTag(),
                Boolean.TRUE.equals(user.getIsAdmin())
        ));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        // 兼容 uni.uploadFile 等无法自定义全部头的场景
        String fallback = request.getHeader("X-Token");
        return fallback == null ? null : fallback.trim();
    }

    private void writeUnauthorized(HttpServletResponse response) throws Exception {
        response.setStatus(200);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":401,\"message\":\"请先登录\",\"data\":null}");
    }
}
