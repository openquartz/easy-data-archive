package com.openquartz.easyarchive.starter.security;

import lombok.Data;

@Data
public class CurrentUserInfo {

    private Long userId;

    private String username;

    private String roleCode;

    public boolean isAdmin() {
        return RoleConstants.isAdmin(roleCode);
    }

    public boolean isArchiveAdmin() {
        return RoleConstants.isArchiveAdmin(roleCode);
    }
}
