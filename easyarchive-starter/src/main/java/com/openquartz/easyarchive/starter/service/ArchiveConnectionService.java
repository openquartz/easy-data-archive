package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;

import java.util.List;

/**
 * 数据源服务
 */
public interface ArchiveConnectionService {

    List<DatasourceTypeOption> listDatasourceTypes();

    List<ArchiveConnection> findAll();

    ArchiveConnection findById(Long id);

    ArchiveConnection create(ArchiveConnection datasource);

    ArchiveConnection update(ArchiveConnection datasource);

    void updateStatus(Long id, Integer status);

    boolean testConnection(ArchiveConnection datasource);
}
