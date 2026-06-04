package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.security.CurrentUserInfo;

import java.util.Set;

public interface DataPermissionService {

    CurrentUserInfo getCurrentUser();

    boolean isAdmin();

    void assertAdmin();

    Set<Long> getAuthorizedDatasourceIds(Long userId);

    void assertDatasourceReadable(Long datasourceId);

    void assertGroupReadable(Long groupId);

    void assertTaskReadable(Long taskId);
}
