package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

@Data
public class UserDatasourceAuthorizationView {
    private Long datasourceId;
    private String datasourceName;
    private String permissionLevel;
    private String grantSource;
}
