package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionPermissionMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.entity.ArchiveConnectionPermission;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserDatasourcePermissionOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserDatasourcePermissionServiceImplTest {

    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final ArchiveConnectionMapper archiveConnectionMapper = mock(ArchiveConnectionMapper.class);
    private final ArchiveConnectionPermissionMapper permissionMapper = mock(ArchiveConnectionPermissionMapper.class);
    private final UserDatasourcePermissionOperationLogPresenter presenter =
            mock(UserDatasourcePermissionOperationLogPresenter.class);
    private final OperationLogRecorder recorder = mock(OperationLogRecorder.class);
    private final UserDatasourcePermissionServiceImpl service = new UserDatasourcePermissionServiceImpl(
            dataPermissionService, datasourceAuthorizationService, sysUserMapper, archiveConnectionMapper, permissionMapper, presenter, recorder);

    @Test
    void shouldGrantManageLevelForArchiveAdmin() {
        when(sysUserMapper.selectById(2L)).thenReturn(archiveAdminUser(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.countByUserIdAndDatasourceIdAndLevel(2L, 3L, "MANAGE")).thenReturn(0);
        when(permissionMapper.selectByUserId(2L))
                .thenReturn(Collections.emptyList(), Collections.singletonList(permissionRecord(3L, "MANAGE")));
        when(presenter.buildGrant(any(), any(), any())).thenReturn(command("GRANT"));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.grant(2L, 3L);

        ArgumentCaptor<ArchiveConnectionPermission> captor = ArgumentCaptor.forClass(ArchiveConnectionPermission.class);
        verify(permissionMapper).insert(captor.capture());
        ArchiveConnectionPermission inserted = captor.getValue();
        assertEquals(2L, inserted.getUserId());
        assertEquals(3L, inserted.getDatasourceId());
        assertEquals("MANAGE", inserted.getPermissionLevel());
        assertEquals("MANUAL_ASSIGN", inserted.getGrantSource());
        assertEquals(1L, inserted.getGrantedByUserId());
        verify(recorder).record(any());
    }

    @Test
    void shouldSkipInsertWhenLevelAlreadyExists() {
        when(sysUserMapper.selectById(2L)).thenReturn(archiveAdminUser(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.countByUserIdAndDatasourceIdAndLevel(2L, 3L, "MANAGE")).thenReturn(1);
        when(permissionMapper.selectByUserId(2L))
                .thenReturn(Collections.singletonList(permissionRecord(3L, "MANAGE")));
        when(presenter.buildGrant(any(), any(), any())).thenReturn(command("GRANT"));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.grant(2L, 3L);

        verify(permissionMapper, never()).insert(any());
        verify(recorder).record(any());
    }

    @Test
    void shouldReplacePermissionsWithManageForArchiveAdmin() {
        when(sysUserMapper.selectById(2L)).thenReturn(archiveAdminUser(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(archiveConnectionMapper.selectById(4L)).thenReturn(datasource(4L));
        when(permissionMapper.selectByUserId(2L))
                .thenReturn(Collections.singletonList(permissionRecord(3L, "MANAGE")),
                        Arrays.asList(permissionRecord(3L, "MANAGE"), permissionRecord(4L, "MANAGE")));
        when(presenter.buildReplace(any(), any(), any())).thenReturn(command("REPLACE"));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.replacePermissions(2L, Arrays.asList(3L, 4L));

        verify(permissionMapper).softDeleteByUserId(2L, "1");
        ArgumentCaptor<List<ArchiveConnectionPermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(permissionMapper).batchInsert(captor.capture());
        List<ArchiveConnectionPermission> inserted = captor.getValue();
        assertEquals(2, inserted.size());
        assertEquals("MANAGE", inserted.get(0).getPermissionLevel());
        assertEquals("MANAGE", inserted.get(1).getPermissionLevel());
        assertEquals(1L, inserted.get(0).getGrantedByUserId());
        assertEquals("MANUAL_ASSIGN", inserted.get(0).getGrantSource());
        verify(recorder).record(any());
    }

    @Test
    void shouldReplacePermissionsWithUseForNormalUser() {
        when(sysUserMapper.selectById(5L)).thenReturn(normalUser(5L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.selectByUserId(5L))
                .thenReturn(Collections.emptyList(), Collections.singletonList(permissionRecord(3L, "USE")));
        when(presenter.buildReplace(any(), any(), any())).thenReturn(command("REPLACE"));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.replacePermissions(5L, Collections.singletonList(3L));

        ArgumentCaptor<List<ArchiveConnectionPermission>> captor = ArgumentCaptor.forClass(List.class);
        verify(permissionMapper).batchInsert(captor.capture());
        assertEquals("USE", captor.getValue().get(0).getPermissionLevel());
    }

    @Test
    void shouldRejectArchiveAdminGrantWithoutManagePermission() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.selectByUserId(2L)).thenReturn(Collections.emptyList());
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentArchiveAdmin());
        when(datasourceAuthorizationService.listDatasourceIdsByLevel(8L, DatasourcePermissionLevelEnum.MANAGE))
                .thenReturn(new HashSet<>(Collections.singletonList(4L)));

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.grant(2L, 3L));

        assertEquals(StarterErrorCode.DATASOURCE_ACCESS_DENIED, error.getErrorCode());
        verify(permissionMapper, never()).insert(any());
    }

    @Test
    void shouldRejectArchiveAdminRevokeWithoutManagePermission() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser(2L));
        when(permissionMapper.selectByUserId(2L))
                .thenReturn(Collections.singletonList(permissionRecord(3L, "USE")));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentArchiveAdmin());
        when(datasourceAuthorizationService.listDatasourceIdsByLevel(8L, DatasourcePermissionLevelEnum.MANAGE))
                .thenReturn(Collections.emptySet());

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.revoke(2L, 3L));

        assertEquals(StarterErrorCode.DATASOURCE_ACCESS_DENIED, error.getErrorCode());
        verify(permissionMapper, never()).softDeleteByUserIdAndDatasourceId(any(), any(), any());
    }

    @Test
    void shouldRecordRevokePermission() {
        when(sysUserMapper.selectById(2L)).thenReturn(archiveAdminUser(2L));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(permissionMapper.selectByUserId(2L))
                .thenReturn(Collections.singletonList(permissionRecord(3L, "MANAGE")), Collections.emptyList());
        when(presenter.buildRevoke(any(), any(), any())).thenReturn(command("REVOKE"));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(dataPermissionService.getCurrentUser()).thenReturn(currentAdmin());

        service.revoke(2L, 3L);

        verify(permissionMapper).softDeleteByUserIdAndDatasourceId(2L, 3L, "1");
        verify(recorder).record(any());
    }

    @Test
    void shouldListAssignedDatasources() {
        when(sysUserMapper.selectById(2L)).thenReturn(archiveAdminUser(2L));
        when(permissionMapper.selectByUserId(2L)).thenReturn(Arrays.asList(
                permissionRecord(3L, "MANAGE"), permissionRecord(4L, "MANAGE")));
        when(archiveConnectionMapper.selectById(3L)).thenReturn(datasource(3L));
        when(archiveConnectionMapper.selectById(4L)).thenReturn(datasource(4L));
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();

        List<ArchiveConnection> result = service.listUserPermissions(2L);

        assertEquals(2, result.size());
    }

    @Test
    void shouldRejectMissingUser() {
        doNothing().when(dataPermissionService).assertAdminOrArchiveAdmin();
        when(sysUserMapper.selectById(9L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.listUserPermissions(9L));
        assertEquals(StarterErrorCode.USER_NOT_FOUND, error.getErrorCode());
        verify(permissionMapper, never()).selectByUserId(eq(9L));
    }

    private static SysUser archiveAdminUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        return user;
    }

    private static SysUser normalUser(Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRoleCode(RoleConstants.NORMAL_USER);
        return user;
    }

    private static ArchiveConnection datasource(Long id) {
        ArchiveConnection connection = new ArchiveConnection();
        connection.setId(id);
        connection.setDatasourceCode("DS_" + id);
        connection.setDatasourceName("数据源-" + id);
        return connection;
    }

    private static ArchiveConnectionPermission permissionRecord(Long datasourceId, String level) {
        ArchiveConnectionPermission permission = new ArchiveConnectionPermission();
        permission.setDatasourceId(datasourceId);
        permission.setPermissionLevel(level);
        return permission;
    }

    private static OperationLogCommand command(String action) {
        return new OperationLogCommand("USER_DATASOURCE_PERMISSION", action, action, "USER", 2L, "user-2", action, null);
    }

    private static CurrentUserInfo currentArchiveAdmin() {
        CurrentUserInfo currentUserInfo = new CurrentUserInfo();
        currentUserInfo.setUserId(8L);
        currentUserInfo.setUsername("archive-admin");
        currentUserInfo.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        return currentUserInfo;
    }

    private static CurrentUserInfo currentAdmin() {
        CurrentUserInfo currentUserInfo = new CurrentUserInfo();
        currentUserInfo.setUserId(1L);
        currentUserInfo.setUsername("admin");
        currentUserInfo.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        return currentUserInfo;
    }
}
