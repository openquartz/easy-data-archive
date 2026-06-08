package com.openquartz.easyarchive.starter.security;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class RoleConstants {

    public static final String PLATFORM_ADMIN = "platform_admin";
    public static final String ARCHIVE_ADMIN = "archive_admin";
    public static final String NORMAL_USER = "normal_user";
    private static final String LEGACY_ADMIN = "ADMIN";
    private static final String LEGACY_USER = "USER";
    private static final Set<String> SUPPORTED_ROLE_CODES = new LinkedHashSet<>(Arrays.asList(
            PLATFORM_ADMIN,
            ARCHIVE_ADMIN,
            NORMAL_USER
    ));

    private RoleConstants() {
    }

    public static boolean isAdmin(String roleCode) {
        return PLATFORM_ADMIN.equals(normalizeRoleCode(roleCode));
    }

    public static boolean isArchiveAdmin(String roleCode) {
        return ARCHIVE_ADMIN.equals(normalizeRoleCode(roleCode));
    }

    public static boolean isNormalUser(String roleCode) {
        return NORMAL_USER.equals(normalizeRoleCode(roleCode));
    }

    public static boolean isSupported(String roleCode) {
        return SUPPORTED_ROLE_CODES.contains(normalizeRoleCode(roleCode));
    }

    public static String defaultRoleCode() {
        return NORMAL_USER;
    }

    public static String normalizeRoleCode(String roleCode) {
        if (roleCode == null) {
            return defaultRoleCode();
        }
        String normalized = roleCode.trim();
        if (normalized.isEmpty()) {
            return defaultRoleCode();
        }
        if (LEGACY_ADMIN.equalsIgnoreCase(normalized)) {
            return PLATFORM_ADMIN;
        }
        if (LEGACY_USER.equalsIgnoreCase(normalized)) {
            return ARCHIVE_ADMIN;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    public static Set<String> supportedRoleCodes() {
        return SUPPORTED_ROLE_CODES;
    }
}
