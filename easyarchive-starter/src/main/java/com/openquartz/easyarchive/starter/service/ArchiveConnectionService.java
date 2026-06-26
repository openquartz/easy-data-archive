package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.model.dto.PageResult;

import java.util.List;

/**
 * 数据源服务
 */
public interface ArchiveConnectionService extends com.openquartz.easyarchive.core.connection.ArchiveConnectionService {

    List<DatasourceTypeOption> listDatasourceTypes();

    List<ArchiveConnection> findAll();

    PageResult<ArchiveConnection> findPage(int page, int size, String keyword, Integer status);

    ArchiveConnection findById(Long id);

    ArchiveConnection create(ArchiveConnection datasource);

    ArchiveConnection update(ArchiveConnection datasource);

    void updateStatus(Long id, Integer status);

    boolean testConnection(ArchiveConnection datasource);
}
