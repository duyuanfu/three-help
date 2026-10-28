package com.bangbangbang.common;

/**
 * 业务异常
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }

    public static BizException of(String message) {
        return new BizException(message);
    }
}
