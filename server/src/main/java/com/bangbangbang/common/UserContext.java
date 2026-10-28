package com.bangbangbang.common;

/**
 * 当前登录用户上下文（由 AuthInterceptor 写入，请求结束后清理）
 */
public final class UserContext {

    public record CurrentUser(String id, String name, String avatar, String phone, String tag,
                              boolean admin) {
    }

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(CurrentUser user) {
        HOLDER.set(user);
    }

    public static CurrentUser get() {
        return HOLDER.get();
    }

    /** 取当前用户，未登录直接抛 401 语义的业务异常 */
    public static CurrentUser require() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            throw BizException.of("登录状态已失效，请重新登录");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
