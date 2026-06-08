package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

@Data
public class UserDatasourceAuthorizationItem {
    private Long datasourceId;
    private String permissionLevel;
}
