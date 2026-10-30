package com.bangbangbang.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web 配置：
 * 1. 跨域（微信开发者工具 / H5 调试）
 * 2. 认证拦截（/api/** 除白名单外均需登录）
 * 3. 上传文件静态访问（/uploads/** → server/uploads/）
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/register",
                        // 微信登录时用户一定还没登录，这两个接口必须在白名单里，
                        // 接口内部靠一次性票据（bind 时）和微信侧 code 校验（login 时）保证安全
                        "/api/auth/wx-login",
                        "/api/auth/wx-bind",
                        "/api/health"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        if (!absolute.endsWith("/")) {
            absolute = absolute + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolute);
    }
}
