package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionPermissionMapper;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatasourceAuthorizationServiceImplTest {

    @Mock
    private ArchiveConnectionPermissionMapper permissionMapper;

    @InjectMocks
    private DatasourceAuthorizationServiceImpl service;

    @Test
    void managePermissionShouldSatisfyUseRequirement() {
        when(permissionMapper.selectLevelsByUserIdAndDatasourceId(8L, 12L))
                .thenReturn(List.of("MANAGE"));
        assertTrue(service.hasPermission(8L, 12L, DatasourcePermissionLevelEnum.USE));
    }

    @Test
    void noPermissionShouldDenyAccess() {
        when(permissionMapper.selectLevelsByUserIdAndDatasourceId(8L, 12L))
                .thenReturn(Collections.emptyList());
        assertFalse(service.hasPermission(8L, 12L, DatasourcePermissionLevelEnum.USE));
    }

    @Test
    void usePermissionShouldNotSatisfyManageRequirement() {
        when(permissionMapper.selectLevelsByUserIdAndDatasourceId(8L, 12L))
                .thenReturn(List.of("USE"));
        assertFalse(service.hasPermission(8L, 12L, DatasourcePermissionLevelEnum.MANAGE));
    }
}
