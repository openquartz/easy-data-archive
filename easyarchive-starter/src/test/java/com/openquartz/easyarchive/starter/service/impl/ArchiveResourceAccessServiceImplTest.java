package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveResourceAccessServiceImplTest {

    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveResourceAccessServiceImpl service = new ArchiveResourceAccessServiceImpl(
            currentUserService, datasourceAuthorizationService, groupMapper, taskMapper);

    // --- assertGroupAccessible ---

    @Test
    void shouldRejectNullGroupId() {
        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.assertGroupAccessible(null));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED, error.getErrorCode());
    }

    @Test
    void shouldSkipPermissionCheckForAdmin() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        service.assertGroupAccessible(10L);

        verify(groupMapper, never()).selectById(10L);
        verify(datasourceAuthorizationService, never()).assertPermission(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldRejectWhenGroupNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(normalUser());
        when(groupMapper.selectById(10L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.assertGroupAccessible(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void shouldCheckUsePermissionOnBothDatasources() {
        when(currentUserService.getCurrentUser()).thenReturn(normalUser());
        when(groupMapper.selectById(10L)).thenReturn(group(10L, 1L, 2L));

        service.assertGroupAccessible(10L);

        verify(datasourceAuthorizationService).assertPermission(2L, 1L, DatasourcePermissionLevelEnum.USE);
        verify(datasourceAuthorizationService).assertPermission(2L, 2L, DatasourcePermissionLevelEnum.USE);
    }

    // --- assertGroupManageable ---

    @Test
    void shouldRejectNullGroupIdForManageable() {
        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.assertGroupManageable(null));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED, error.getErrorCode());
    }

    @Test
    void shouldSkipManagePermissionCheckForAdmin() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        service.assertGroupManageable(10L);

        verify(groupMapper, never()).selectById(10L);
    }

    @Test
    void shouldCheckManagePermissionOnBothDatasources() {
        when(currentUserService.getCurrentUser()).thenReturn(normalUser());
        when(groupMapper.selectById(10L)).thenReturn(group(10L, 1L, 2L));

        service.assertGroupManageable(10L);

        verify(datasourceAuthorizationService).assertPermission(2L, 1L, DatasourcePermissionLevelEnum.MANAGE);
        verify(datasourceAuthorizationService).assertPermission(2L, 2L, DatasourcePermissionLevelEnum.MANAGE);
    }

    @Test
    void shouldAllowNormalUserToManageOwnedGroup() {
        when(currentUserService.getCurrentUser()).thenReturn(normalUser());
        when(groupMapper.selectById(10L)).thenReturn(group(10L, 1L, 2L, 2L));

        service.assertGroupManageable(10L);

        verify(datasourceAuthorizationService, never()).assertPermission(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any());
    }

    // --- assertTaskAccessible ---

    @Test
    void shouldRejectNullTaskId() {
        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.assertTaskAccessible(null));
        assertEquals(StarterErrorCode.TASK_ID_REQUIRED, error.getErrorCode());
    }

    @Test
    void shouldSkipTaskPermissionCheckForAdmin() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser());

        service.assertTaskAccessible(11L);

        verify(taskMapper, never()).selectById(11L);
    }

    @Test
    void shouldRejectWhenTaskNotFound() {
        when(currentUserService.getCurrentUser()).thenReturn(normalUser());
        when(taskMapper.selectById(11L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.assertTaskAccessible(11L));
        assertEquals(StarterErrorCode.TASK_NOT_FOUND, error.getErrorCode());
    }

    @Test
    void shouldDelegateToGroupAccessibleForTask() {
        when(currentUserService.getCurrentUser()).thenReturn(normalUser());
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(11L);
        task.setGroupId(10L);
        when(taskMapper.selectById(11L)).thenReturn(task);
        when(groupMapper.selectById(10L)).thenReturn(group(10L, 1L, 2L));

        service.assertTaskAccessible(11L);

        verify(datasourceAuthorizationService).assertPermission(2L, 1L, DatasourcePermissionLevelEnum.USE);
        verify(datasourceAuthorizationService).assertPermission(2L, 2L, DatasourcePermissionLevelEnum.USE);
    }

    // --- helpers ---

    private static CurrentUserInfo adminUser() {
        CurrentUserInfo info = new CurrentUserInfo();
        info.setUserId(1L);
        info.setUsername("admin");
        info.setRoleCode("platform_admin");
        return info;
    }

    private static CurrentUserInfo normalUser() {
        CurrentUserInfo info = new CurrentUserInfo();
        info.setUserId(2L);
        info.setUsername("user");
        info.setRoleCode("normal_user");
        return info;
    }

    private static ArchiveGroup group(Long id, Long sourceDatasourceId, Long targetDatasourceId) {
        return group(id, sourceDatasourceId, targetDatasourceId, null);
    }

    private static ArchiveGroup group(Long id, Long sourceDatasourceId, Long targetDatasourceId, Long ownerUserId) {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(id);
        group.setSourceDatasourceId(sourceDatasourceId);
        group.setTargetDatasourceId(targetDatasourceId);
        group.setOwnerUserId(ownerUserId);
        return group;
    }
}
