package com.openquartz.easyarchive.starter.model.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserDatasourcePermission extends BaseEntity {

    private Long id;

    private Long userId;

    private Long datasourceId;

    private String permissionType;
}
