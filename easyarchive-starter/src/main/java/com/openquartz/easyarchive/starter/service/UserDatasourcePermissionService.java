package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;

import java.util.List;

public interface UserDatasourcePermissionService {

    List<ArchiveConnection> listUserPermissions(Long userId);

    void grant(Long userId, Long datasourceId);

    void revoke(Long userId, Long datasourceId);

    void replacePermissions(Long userId, List<Long> datasourceIds);
}
