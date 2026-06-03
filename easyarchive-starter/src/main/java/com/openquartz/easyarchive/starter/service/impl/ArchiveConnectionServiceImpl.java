package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.model.enums.DatasourceTypeEnum;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.ArchiveConnectionService;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.support.DatasourceConnectionTester;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据源服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveConnectionServiceImpl implements ArchiveConnectionService {

    private final ArchiveConnectionMapper datasourceMapper;
    private final DatasourceConnectionTester connectionTester;
    private final DataPermissionService dataPermissionService;
    private final DatasourceOperationLogPresenter datasourceOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<DatasourceTypeOption> listDatasourceTypes() {
        return DatasourceTypeEnum.toOptions();
    }

    @Override
    public List<ArchiveConnection> findAll() {
        if (dataPermissionService.isAdmin()) {
            return datasourceMapper.selectList(null, null);
        }
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        return datasourceMapper.selectAuthorizedList(userId, null);
    }

    @Override
    public ArchiveConnection findById(Long id) {
        if (dataPermissionService.isAdmin()) {
            return datasourceMapper.selectById(id);
        }
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        return datasourceMapper.selectAuthorizedById(userId, id);
    }

    @Override
    public ArchiveConnection create(ArchiveConnection datasource) {
        dataPermissionService.assertAdmin();
        datasourceMapper.insert(datasource);
        operationLogRecorder.record(datasourceOperationLogPresenter.buildCreate(datasource));
        return datasource;
    }

    @Override
    public ArchiveConnection update(ArchiveConnection datasource) {
        dataPermissionService.assertAdmin();
        ArchiveConnection before = datasourceMapper.selectById(datasource.getId());
        datasourceMapper.update(datasource);
        ArchiveConnection after = datasourceMapper.selectById(datasource.getId());
        operationLogRecorder.record(datasourceOperationLogPresenter.buildUpdate(before, after));
        return datasource;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        dataPermissionService.assertAdmin();
        ArchiveConnection before = datasourceMapper.selectById(id);
        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(id);
        datasource.setStatus(status);
        datasourceMapper.update(datasource);
        ArchiveConnection after = datasourceMapper.selectById(id);
        operationLogRecorder.record(datasourceOperationLogPresenter.buildStatusUpdate(before, after));
    }

    @Override
    public boolean testConnection(ArchiveConnection datasource) {
        dataPermissionService.assertAdmin();
        boolean result = connectionTester.testConnection(datasource);
        operationLogRecorder.record(datasourceOperationLogPresenter.buildTestConnection(datasource, result));
        return result;
    }
}
