package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.core.common.SysUser;

import java.util.Set;

public interface DatasourceAuthorizationService {
    boolean hasPermission(Long userId, Long datasourceId, DatasourcePermissionLevelEnum requiredLevel);
    void assertPermission(Long userId, Long datasourceId, DatasourcePermissionLevelEnum requiredLevel);
    Set<Long> listDatasourceIdsByLevel(Long userId, DatasourcePermissionLevelEnum level);
    void assertGrantable(CurrentUserInfo operator, SysUser target, Long datasourceId, DatasourcePermissionLevelEnum level);
}
