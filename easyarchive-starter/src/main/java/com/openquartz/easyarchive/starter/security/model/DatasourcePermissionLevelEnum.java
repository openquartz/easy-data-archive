package com.openquartz.easyarchive.starter.security.model;

public enum DatasourcePermissionLevelEnum {
    MANAGE,
    USE;

    public boolean covers(DatasourcePermissionLevelEnum required) {
        return this == MANAGE || this == required;
    }
}
