package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.common.enums.DatasourceStatusEnum;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.model.enums.DatasourceTypeEnum;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.starter.service.ArchiveConnectionService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import com.openquartz.easyarchive.starter.support.DatasourceConnectionTester;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 数据源服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveConnectionServiceImpl implements ArchiveConnectionService {

    private final ArchiveConnectionMapper datasourceMapper;
    private final DatasourceConnectionTester connectionTester;
    private final CurrentUserService currentUserService;
    private final DatasourceAuthorizationService datasourceAuthorizationService;
    private final DatasourceOperationLogPresenter datasourceOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<DatasourceTypeOption> listDatasourceTypes() {
        return DatasourceTypeEnum.toOptions();
    }

    @Override
    public List<ArchiveConnection> findAll() {
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            return datasourceMapper.selectList(null, null);
        }
        DatasourcePermissionLevelEnum level = RoleConstants.isArchiveAdmin(currentUser.getRoleCode())
                ? DatasourcePermissionLevelEnum.MANAGE : DatasourcePermissionLevelEnum.USE;
        Set<Long> ids = datasourceAuthorizationService.listDatasourceIdsByLevel(currentUser.getUserId(), level);
        return ids.isEmpty() ? Collections.emptyList() : datasourceMapper.selectAuthorizedListByIds(ids);
    }

    @Override
    public ArchiveConnection findById(Long id) {
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            return datasourceMapper.selectById(id);
        }
        datasourceAuthorizationService.assertPermission(currentUser.getUserId(), id, DatasourcePermissionLevelEnum.USE);
        return datasourceMapper.selectById(id);
    }

    @Override
    public ArchiveConnection getByConnectionCode(String connectionCode) {
        return datasourceMapper.selectByCode(connectionCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveConnection create(ArchiveConnection datasource) {
        currentUserService.assertAdmin();
        if (datasource.getStatus() == null || !DatasourceStatusEnum.DISABLED.getCode().equals(datasource.getStatus())) {
            datasource.setStatus(DatasourceStatusEnum.UNTESTED.getCode());
        }
        datasourceMapper.insert(datasource);
        operationLogRecorder.record(datasourceOperationLogPresenter.buildCreate(datasource));
        return datasource;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveConnection update(ArchiveConnection datasource) {
        currentUserService.assertAdmin();
        ArchiveConnection before = datasourceMapper.selectById(datasource.getId());
        if (before == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
        }
        if (isConnectionConfigChanged(before, datasource)) {
            datasource.setStatus(DatasourceStatusEnum.UNTESTED.getCode());
        } else if (datasource.getStatus() == null) {
            datasource.setStatus(before.getStatus());
        }
        if (!hasText(datasource.getPasswordCipher())) {
            datasource.setPasswordCipher(before.getPasswordCipher());
        }
        datasourceMapper.update(datasource);
        ArchiveConnection after = datasourceMapper.selectById(datasource.getId());
        operationLogRecorder.record(datasourceOperationLogPresenter.buildUpdate(before, after));
        return after;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        currentUserService.assertAdmin();
        ArchiveConnection before = datasourceMapper.selectById(id);
        if (before == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
        }
        validateStatusForManualUpdate(status);
        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(id);
        datasource.setStatus(status);
        datasourceMapper.update(datasource);
        ArchiveConnection after = datasourceMapper.selectById(id);
        operationLogRecorder.record(datasourceOperationLogPresenter.buildStatusUpdate(before, after));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean testConnection(ArchiveConnection datasource) {
        currentUserService.assertAdmin();
        ArchiveConnection target = prepareDatasourceForTest(datasource);
        boolean result = connectionTester.testConnection(target);
        if (target.getId() != null) {
            ArchiveConnection update = new ArchiveConnection();
            update.setId(target.getId());
            update.setLastCheckTime(new Date());
            if (result) {
                update.setStatus(DatasourceStatusEnum.ENABLED.getCode());
            }
            datasourceMapper.update(update);
        }
        operationLogRecorder.record(datasourceOperationLogPresenter.buildTestConnection(target, result));
        return result;
    }

    private void validateStatusForManualUpdate(Integer status) {
        if (status == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_STATUS_REQUIRED);
        }
        if (!DatasourceStatusEnum.DISABLED.getCode().equals(status)) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_STATUS_MANUAL_UPDATE_UNSUPPORTED);
        }
    }

    private ArchiveConnection prepareDatasourceForTest(ArchiveConnection datasource) {
        if (datasource == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_REQUIRED);
        }
        if (datasource.getId() == null) {
            return datasource;
        }
        ArchiveConnection persisted = datasourceMapper.selectById(datasource.getId());
        if (persisted == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
        }
        ArchiveConnection merged = new ArchiveConnection();
        merged.setId(persisted.getId());
        merged.setDatasourceCode(hasText(datasource.getDatasourceCode()) ? datasource.getDatasourceCode() : persisted.getDatasourceCode());
        merged.setDatasourceName(hasText(datasource.getDatasourceName()) ? datasource.getDatasourceName() : persisted.getDatasourceName());
        merged.setDatasourceType(hasText(datasource.getDatasourceType()) ? datasource.getDatasourceType() : persisted.getDatasourceType());
        merged.setJdbcUrl(hasText(datasource.getJdbcUrl()) ? datasource.getJdbcUrl() : persisted.getJdbcUrl());
        merged.setUsername(hasText(datasource.getUsername()) ? datasource.getUsername() : persisted.getUsername());
        merged.setPasswordCipher(hasText(datasource.getPasswordCipher()) ? datasource.getPasswordCipher() : persisted.getPasswordCipher());
        merged.setStatus(persisted.getStatus());
        return merged;
    }

    private boolean isConnectionConfigChanged(ArchiveConnection before, ArchiveConnection after) {
        return !Objects.equals(before.getJdbcUrl(), after.getJdbcUrl())
                || !Objects.equals(before.getUsername(), after.getUsername())
                || hasText(after.getPasswordCipher());
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
