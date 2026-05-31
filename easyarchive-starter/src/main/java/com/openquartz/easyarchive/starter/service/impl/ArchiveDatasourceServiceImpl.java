package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.mapper.EaArchiveDatasourceMapper;
import com.openquartz.easyarchive.starter.model.entity.EaArchiveDatasource;
import com.openquartz.easyarchive.starter.service.ArchiveDatasourceService;
import com.openquartz.easyarchive.starter.support.DatasourceConnectionTester;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 数据源服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveDatasourceServiceImpl implements ArchiveDatasourceService {

    private final EaArchiveDatasourceMapper datasourceMapper;
    private final DatasourceConnectionTester connectionTester;

    @Override
    public List<EaArchiveDatasource> findAll() {
        return datasourceMapper.selectList(null, null);
    }

    @Override
    public EaArchiveDatasource findById(Long id) {
        return datasourceMapper.selectById(id);
    }

    @Override
    public EaArchiveDatasource create(EaArchiveDatasource datasource) {
        datasourceMapper.insert(datasource);
        return datasource;
    }

    @Override
    public EaArchiveDatasource update(EaArchiveDatasource datasource) {
        datasourceMapper.update(datasource);
        return datasource;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        EaArchiveDatasource datasource = new EaArchiveDatasource();
        datasource.setId(id);
        datasource.setStatus(status);
        datasourceMapper.update(datasource);
    }

    @Override
    public boolean testConnection(EaArchiveDatasource datasource) {
        return connectionTester.testConnection(datasource);
    }
}