package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.model.entity.EaArchiveDatasource;

import java.util.List;

/**
 * 数据源服务
 */
public interface ArchiveDatasourceService {

    List<EaArchiveDatasource> findAll();

    EaArchiveDatasource findById(Long id);

    EaArchiveDatasource create(EaArchiveDatasource datasource);

    EaArchiveDatasource update(EaArchiveDatasource datasource);

    void updateStatus(Long id, Integer status);

    boolean testConnection(EaArchiveDatasource datasource);
}