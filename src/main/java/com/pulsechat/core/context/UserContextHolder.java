package com.pulsechat.core.context;

public final class UserContextHolder {

    private static final ThreadLocal<String> CURRENT_USER = new ThreadLocal<>();

    private UserContextHolder() {}

    public static void setUserId(String userId) {
        CURRENT_USER.set(userId);
    }

    public static String getUserId() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
