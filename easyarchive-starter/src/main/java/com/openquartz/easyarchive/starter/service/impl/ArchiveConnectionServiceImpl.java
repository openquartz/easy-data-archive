package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.model.enums.DatasourceTypeEnum;
import com.openquartz.easyarchive.starter.service.ArchiveConnectionService;
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

    @Override
    public List<DatasourceTypeOption> listDatasourceTypes() {
        return DatasourceTypeEnum.toOptions();
    }

    @Override
    public List<ArchiveConnection> findAll() {
        return datasourceMapper.selectList(null, null);
    }

    @Override
    public ArchiveConnection findById(Long id) {
        return datasourceMapper.selectById(id);
    }

    @Override
    public ArchiveConnection create(ArchiveConnection datasource) {
        datasourceMapper.insert(datasource);
        return datasource;
    }

    @Override
    public ArchiveConnection update(ArchiveConnection datasource) {
        datasourceMapper.update(datasource);
        return datasource;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(id);
        datasource.setStatus(status);
        datasourceMapper.update(datasource);
    }

    @Override
    public boolean testConnection(ArchiveConnection datasource) {
        return connectionTester.testConnection(datasource);
    }
}
