package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReplaceUserDatasourcePermissionsRequest {

    private List<Long> datasourceIds;
}
