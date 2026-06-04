package com.openquartz.easyarchive.starter.security;

import lombok.Data;

@Data
public class CurrentUserInfo {

    private Long userId;

    private String username;

    private String roleCode;

    public boolean isAdmin() {
        return RoleConstants.ADMIN.equalsIgnoreCase(roleCode);
    }
}
