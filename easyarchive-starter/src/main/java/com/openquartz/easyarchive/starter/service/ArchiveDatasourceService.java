package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;

import java.util.List;

/**
 * 数据源服务
 */
public interface ArchiveDatasourceService {

    List<ArchiveConnection> findAll();

    ArchiveConnection findById(Long id);

    ArchiveConnection create(ArchiveConnection datasource);

    ArchiveConnection update(ArchiveConnection datasource);

    void updateStatus(Long id, Integer status);

    boolean testConnection(ArchiveConnection datasource);
}
