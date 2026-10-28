package com.bangbangbang.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：保证前端永远拿到 JSON，而不是 HTML 错误页
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResult<Void> handleBiz(BizException e) {
        log.warn("[业务异常] {}", e.getMessage());
        return ApiResult.fail(e.getMessage());
    }

    /** 请求体反序列化失败（如把手机号填进数字字段导致数值超出 int 范围），给人话提示 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResult<Void> handleUnreadable(HttpMessageNotReadableException e) {
        log.warn("[请求体解析失败] {}", e.getMessage());
        return ApiResult.fail("请求参数格式不正确，请检查填写的内容");
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleOther(Exception e) {
        log.error("[系统异常]", e);
        return ApiResult.fail("服务异常：" + e.getMessage());
    }
}
