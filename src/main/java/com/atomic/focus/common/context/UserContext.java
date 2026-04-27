package com.atomic.focus.common.context;

/**
 * 当前请求的用户上下文（基于 ThreadLocal 实现）。
 * 由 AuthInterceptor 在请求开始时写入，结束后清除。
 */
public final class UserContext {

    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IS_GUEST = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String userId, boolean isGuest) {
        USER_ID.set(userId);
        IS_GUEST.set(isGuest);
    }

    public static String getUserId() {
        return USER_ID.get();
    }

    public static String requireUserId() {
        String userId = USER_ID.get();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("当前请求未登录");
        }
        return userId;
    }

    public static boolean isGuest() {
        Boolean v = IS_GUEST.get();
        return v != null && v;
    }

    public static void clear() {
        USER_ID.remove();
        IS_GUEST.remove();
    }
}
