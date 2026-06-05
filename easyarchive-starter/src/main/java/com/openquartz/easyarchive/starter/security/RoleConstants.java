package com.openquartz.easyarchive.starter.security;

public final class RoleConstants {

    public static final String ADMIN = "platform_admin";
    public static final String USER = "archive_admin";
    private static final String LEGACY_ADMIN = "ADMIN";
    private static final String LEGACY_USER = "USER";

    private RoleConstants() {
    }

    public static boolean isAdmin(String roleCode) {
        return matches(roleCode, ADMIN, LEGACY_ADMIN);
    }

    public static boolean isUser(String roleCode) {
        return matches(roleCode, USER, LEGACY_USER);
    }

    private static boolean matches(String roleCode, String primary, String legacy) {
        return primary.equalsIgnoreCase(roleCode) || legacy.equalsIgnoreCase(roleCode);
    }
}
