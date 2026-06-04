package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.mapper.UserDatasourcePermissionMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserDatasourcePermissionOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDatasourcePermissionServiceImplTest {

    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final ArchiveConnectionMapper archiveConnectionMapper = mock(ArchiveConnectionMapper.class);
    private final UserDatasourcePermissionMapper permissionMapper = mock(UserDatasourcePermissionMapper.class);
    private final UserDatasourcePermissionOperationLogPresenter presenter =
            mock(UserDatasourcePermissionOperationLogPresenter.class);
    private final OperationLogRecorder recorder = mock(OperationLogRecorder.class);
    private final UserDatasourcePermissionServiceImpl service = new UserDatasourcePermissionServiceImpl(
            dataPermissionService, sysUserMapper, archiveConnectionMapper, permissionMapper, presenter, recorder);

    @Test
    void shouldGrantPermission() {
        when(sysUserMapper.selectById(2L)).thenReturn(user(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.countByUserIdAndDatasourceId(2L, 3L, "READ")).thenReturn(0);
        when(permissionMapper.selectDatasourceIdsByUserId(2L, "READ")).thenReturn(Collections.emptyList(), Arrays.asList(3L));
        when(presenter.buildGrant(any(), any(), any())).thenReturn(command("GRANT"));
        doNothing().when(dataPermissionService).assertAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.grant(2L, 3L);

        verify(permissionMapper).insert(any());
        verify(recorder).record(any());
    }

    @Test
    void shouldReplacePermissions() {
        when(sysUserMapper.selectById(2L)).thenReturn(user(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(archiveConnectionMapper.selectById(4L)).thenReturn(datasource(4L));
        when(permissionMapper.selectDatasourceIdsByUserId(2L, "READ")).thenReturn(Arrays.asList(3L), Arrays.asList(3L, 4L));
        when(presenter.buildReplace(any(), any(), any())).thenReturn(command("REPLACE"));
        doNothing().when(dataPermissionService).assertAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.replacePermissions(2L, Arrays.asList(3L, 4L));

        verify(permissionMapper).deleteByUserId(2L, "1");
        verify(permissionMapper).batchInsert(any());
        verify(recorder).record(any());
    }

    @Test
    void shouldRecordRevokePermission() {
        when(sysUserMapper.selectById(2L)).thenReturn(user(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.selectDatasourceIdsByUserId(2L, "READ")).thenReturn(Arrays.asList(3L), Collections.emptyList());
        when(presenter.buildRevoke(any(), any(), any())).thenReturn(command("REVOKE"));
        doNothing().when(dataPermissionService).assertAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.revoke(2L, 3L);

        verify(permissionMapper).deleteByUserIdAndDatasourceId(2L, 3L, "1");
        verify(recorder).record(any());
    }

    @Test
    void shouldListAssignedDatasources() {
        when(sysUserMapper.selectById(2L)).thenReturn(user(2L));
        when(permissionMapper.selectDatasourceIdsByUserId(2L, "READ")).thenReturn(Arrays.asList(3L, 4L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(archiveConnectionMapper.selectById(4L)).thenReturn(datasource(4L));
        doNothing().when(dataPermissionService).assertAdmin();

        List<ArchiveConnection> result = service.listUserPermissions(2L);

        assertEquals(2, result.size());
    }

    @Test
    void shouldRejectMissingUser() {
        doNothing().when(dataPermissionService).assertAdmin();
        when(sysUserMapper.selectById(9L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.listUserPermissions(9L));
        assertEquals(StarterErrorCode.USER_NOT_FOUND, error.getErrorCode());
        verify(permissionMapper, never()).selectDatasourceIdsByUserId(9L, "READ");
    }

    private static SysUser user(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user-" + id);
        return user;
    }

    private static ArchiveConnection datasource(Long id) {
        ArchiveConnection connection = new ArchiveConnection();
        connection.setId(id);
        connection.setDatasourceCode("DS_" + id);
        connection.setDatasourceName("数据源-" + id);
        return connection;
    }

    private static OperationLogCommand command(String action) {
        return new OperationLogCommand("USER_DATASOURCE_PERMISSION", action, action, "USER", 2L, "user-2", action, null);
    }

    private static CurrentUserInfo currentAdmin() {
        CurrentUserInfo currentUserInfo = new CurrentUserInfo();
        currentUserInfo.setUserId(1L);
        currentUserInfo.setUsername("admin");
        currentUserInfo.setRoleCode("ADMIN");
        return currentUserInfo;
    }
}
