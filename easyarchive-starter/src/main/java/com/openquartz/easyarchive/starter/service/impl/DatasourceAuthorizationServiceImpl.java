package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionPermissionMapper;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DatasourceAuthorizationServiceImpl implements DatasourceAuthorizationService {

    private final ArchiveConnectionPermissionMapper permissionMapper;

    @Override
    public boolean hasPermission(Long userId, Long datasourceId, DatasourcePermissionLevelEnum requiredLevel) {
        List<String> levels = permissionMapper.selectLevelsByUserIdAndDatasourceId(userId, datasourceId);
        for (String level : levels) {
            DatasourcePermissionLevelEnum owned = DatasourcePermissionLevelEnum.valueOf(level);
            if (owned.covers(requiredLevel)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void assertPermission(Long userId, Long datasourceId, DatasourcePermissionLevelEnum requiredLevel) {
        if (!hasPermission(userId, datasourceId, requiredLevel)) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_ACCESS_DENIED);
        }
    }

    @Override
    public Set<Long> listDatasourceIdsByLevel(Long userId, DatasourcePermissionLevelEnum level) {
        Set<Long> ids = permissionMapper.selectDatasourceIdsByUserIdAndLevel(userId, level.name());
        return ids == null ? Collections.emptySet() : ids;
    }

    @Override
    public void assertGrantable(CurrentUserInfo operator, SysUser target, Long datasourceId, DatasourcePermissionLevelEnum level) {
        if (RoleConstants.isAdmin(operator.getRoleCode())) {
            return;
        }
        if (!hasPermission(operator.getUserId(), datasourceId, DatasourcePermissionLevelEnum.MANAGE)) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_ACCESS_DENIED);
        }
    }
}
