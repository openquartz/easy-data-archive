package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.mapper.UserDatasourcePermissionMapper;
import com.openquartz.easyarchive.starter.model.entity.UserDatasourcePermission;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserDatasourcePermissionOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.service.UserDatasourcePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDatasourcePermissionServiceImpl implements UserDatasourcePermissionService {

    private static final String READ_PERMISSION = "READ";

    private final DataPermissionService dataPermissionService;
    private final SysUserMapper sysUserMapper;
    private final ArchiveConnectionMapper archiveConnectionMapper;
    private final UserDatasourcePermissionMapper permissionMapper;
    private final UserDatasourcePermissionOperationLogPresenter userDatasourcePermissionOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<ArchiveConnection> listUserPermissions(Long userId) {
        dataPermissionService.assertAdmin();
        ensureUserExists(userId);
        List<Long> datasourceIds = permissionMapper.selectDatasourceIdsByUserId(userId, READ_PERMISSION);
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
        dataPermissionService.assertAdmin();
        SysUser user = ensureUserExists(userId);
        List<String> beforeNames = listDatasourceNames(userId);
        ensureDatasourceExists(datasourceId);
        if (permissionMapper.countByUserIdAndDatasourceId(userId, datasourceId, READ_PERMISSION) > 0) {
            operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildGrant(user, beforeNames, beforeNames));
            return;
        }

        String operatorId = String.valueOf(dataPermissionService.getCurrentUser().getUserId());
        UserDatasourcePermission permission = new UserDatasourcePermission();
        permission.setUserId(userId);
        permission.setDatasourceId(datasourceId);
        permission.setPermissionType(READ_PERMISSION);
        permission.setCreatorId(operatorId);
        permission.setUpdaterId(operatorId);
        permissionMapper.insert(permission);
        operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildGrant(
                user, beforeNames, listDatasourceNames(userId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(Long userId, Long datasourceId) {
        dataPermissionService.assertAdmin();
        SysUser user = ensureUserExists(userId);
        List<String> beforeNames = listDatasourceNames(userId);
        String operatorId = String.valueOf(dataPermissionService.getCurrentUser().getUserId());
        permissionMapper.deleteByUserIdAndDatasourceId(userId, datasourceId, operatorId);
        operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildRevoke(
                user, beforeNames, listDatasourceNames(userId)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePermissions(Long userId, List<Long> datasourceIds) {
        dataPermissionService.assertAdmin();
        SysUser user = ensureUserExists(userId);
        List<String> beforeNames = listDatasourceNames(userId);
        String operatorId = String.valueOf(dataPermissionService.getCurrentUser().getUserId());
        permissionMapper.deleteByUserId(userId, operatorId);
        if (datasourceIds == null || datasourceIds.isEmpty()) {
            operationLogRecorder.record(userDatasourcePermissionOperationLogPresenter.buildReplace(
                    user, beforeNames, Collections.emptyList()));
            return;
        }

        List<UserDatasourcePermission> permissions = new ArrayList<>(datasourceIds.size());
        for (Long datasourceId : datasourceIds) {
            ensureDatasourceExists(datasourceId);
            UserDatasourcePermission permission = new UserDatasourcePermission();
            permission.setUserId(userId);
            permission.setDatasourceId(datasourceId);
            permission.setPermissionType(READ_PERMISSION);
            permission.setCreatorId(operatorId);
            permission.setUpdaterId(operatorId);
            permissions.add(permission);
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

    private List<String> listDatasourceNames(Long userId) {
        List<Long> datasourceIds = permissionMapper.selectDatasourceIdsByUserId(userId, READ_PERMISSION);
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
