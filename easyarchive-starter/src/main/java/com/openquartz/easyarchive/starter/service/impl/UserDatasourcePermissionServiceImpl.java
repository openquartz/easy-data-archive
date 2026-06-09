package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionPermissionMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.entity.ArchiveConnectionPermission;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserDatasourcePermissionOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.starter.security.model.GrantSourceEnum;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import com.openquartz.easyarchive.starter.service.UserDatasourcePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserDatasourcePermissionServiceImpl implements UserDatasourcePermissionService {

    private final DataPermissionService dataPermissionService;
    private final DatasourceAuthorizationService datasourceAuthorizationService;
    private final SysUserMapper sysUserMapper;
    private final ArchiveConnectionMapper archiveConnectionMapper;
    private final ArchiveConnectionPermissionMapper permissionMapper;
    private final UserDatasourcePermissionOperationLogPresenter userDatasourcePermissionOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<ArchiveConnection> listUserPermissions(Long userId) {
        dataPermissionService.assertAdminOrArchiveAdmin();
        ensureUserExists(userId);
        Set<Long> datasourceIds = listAuthorizedDatasourceIds(userId);
        if (datasourceIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ArchiveConnection> result = new ArrayList<>(datasourceIds.size());
        for (Long datasourceId : datasourceIds) {
            ArchiveConnection datasource = archiveConnectionMapper.selectById(datasourceId);
            if (datasource != null) {
                result.add(datasource);
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(Long userId, Long datasourceId) {
        dataPermissionService.assertAdminOrArchiveAdmin();
        SysUser user = ensureUserExists(userId);
        List<String> beforeNames = listDatasourceNames(userId);
        ensureDatasourceExists(datasourceId);
        ensureOperatorCanManageDatasource(datasourceId);
        DatasourcePermissionLevelEnum level = resolveLevel(user);
        if (permissionMapper.countByUserIdAndDatasourceIdAndLevel(userId, datasourceId, level.name()) > 0) {
            operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildGrant(user, beforeNames, beforeNames));
            return;
        }

        CurrentUserInfo operator = dataPermissionService.getCurrentUser();
        ArchiveConnectionPermission permission = buildPermission(userId, datasourceId, level, operator.getUserId());
        permissionMapper.insert(permission);
        operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildGrant(
                user, beforeNames, listDatasourceNames(userId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long userId, Long datasourceId) {
        dataPermissionService.assertAdminOrArchiveAdmin();
        SysUser user = ensureUserExists(userId);
        List<String> beforeNames = listDatasourceNames(userId);
        ensureOperatorCanManageDatasource(datasourceId);
        String operatorId = String.valueOf(dataPermissionService.getCurrentUser().getUserId());
        permissionMapper.softDeleteByUserIdAndDatasourceId(userId, datasourceId, operatorId);
        operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildRevoke(
                user, beforeNames, listDatasourceNames(userId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Long userId, List<Long> datasourceIds) {
        dataPermissionService.assertAdminOrArchiveAdmin();
        CurrentUserInfo operator = dataPermissionService.getCurrentUser();
        if (datasourceIds != null && !datasourceIds.isEmpty()) {
            ensureOperatorCanManageDatasources(datasourceIds);
        }
        SysUser user = ensureUserExists(userId);
        List<String> beforeNames = listDatasourceNames(userId);
        String operatorId = String.valueOf(operator.getUserId());
        permissionMapper.softDeleteByUserId(userId, operatorId);
        if (datasourceIds == null || datasourceIds.isEmpty()) {
            operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildReplace(
                    user, beforeNames, Collections.emptyList()));
            return;
        }

        DatasourcePermissionLevelEnum level = resolveLevel(user);
        List<ArchiveConnectionPermission> permissions = new ArrayList<>(datasourceIds.size());
        for (Long datasourceId : datasourceIds) {
            ensureDatasourceExists(datasourceId);
            permissions.add(buildPermission(userId, datasourceId, level, operator.getUserId()));
        }
        permissionMapper.batchInsert(permissions);
        operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildReplace(
                user, beforeNames, listDatasourceNames(userId)));
    }

    private SysUser ensureUserExists(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new StarterManageException(StarterErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private void ensureDatasourceExists(Long datasourceId) {
        ArchiveConnection datasource = archiveConnectionMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
        }
    }

    private void ensureOperatorCanManageDatasource(Long datasourceId) {
        ensureOperatorCanManageDatasources(Collections.singletonList(datasourceId));
    }

    private void ensureOperatorCanManageDatasources(List<Long> datasourceIds) {
        CurrentUserInfo operator = dataPermissionService.getCurrentUser();
        if (!operator.isArchiveAdmin()) {
            return;
        }
        Set<Long> manageableIds = datasourceAuthorizationService
                .listDatasourceIdsByLevel(operator.getUserId(), DatasourcePermissionLevelEnum.MANAGE);
        for (Long datasourceId : datasourceIds) {
            if (!manageableIds.contains(datasourceId)) {
                throw new StarterManageException(StarterErrorCode.DATASOURCE_ACCESS_DENIED);
            }
        }
    }

    private DatasourcePermissionLevelEnum resolveLevel(SysUser user) {
        return RoleConstants.isArchiveAdmin(user.getRoleCode())
                ? DatasourcePermissionLevelEnum.MANAGE
                : DatasourcePermissionLevelEnum.USE;
    }

    private ArchiveConnectionPermission buildPermission(Long userId, Long datasourceId,
                                                        DatasourcePermissionLevelEnum level, Long operatorId) {
        ArchiveConnectionPermission permission = new ArchiveConnectionPermission();
        permission.setUserId(userId);
        permission.setDatasourceId(datasourceId);
        permission.setPermissionLevel(level.name());
        permission.setGrantSource(GrantSourceEnum.MANUAL_ASSIGN.name());
        permission.setGrantedByUserId(operatorId);
        return permission;
    }

    private Set<Long> listAuthorizedDatasourceIds(Long userId) {
        List<ArchiveConnectionPermission> permissions = permissionMapper.selectByUserId(userId);
        if (permissions == null || permissions.isEmpty()) {
            return Collections.emptySet();
        }
        Set<Long> ids = new LinkedHashSet<>(permissions.size());
        for (ArchiveConnectionPermission permission : permissions) {
            ids.add(permission.getDatasourceId());
        }
        return ids;
    }

    private List<String> listDatasourceNames(Long userId) {
        Set<Long> datasourceIds = listAuthorizedDatasourceIds(userId);
        if (datasourceIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>(datasourceIds.size());
        for (Long datasourceId : datasourceIds) {
            ArchiveConnection datasource = archiveConnectionMapper.selectById(datasourceId);
            if (datasource != null) {
                names.add(datasource.getDatasourceName() != null ? datasource.getDatasourceName() : datasource.getDatasourceCode());
            }
        }
        return names;
    }
}
