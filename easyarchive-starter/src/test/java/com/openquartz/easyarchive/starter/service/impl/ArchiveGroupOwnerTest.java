package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.model.dto.PageResult;
import com.openquartz.easyarchive.starter.notification.inapp.ArchiveInAppNotificationService;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveGroupOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveGroupOwnerTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveConnectionMapper archiveConnectionMapper = mock(ArchiveConnectionMapper.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveGroupItemByIdMapper idItemMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeItemMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ArchiveResourceAccessService archiveResourceAccessService = mock(ArchiveResourceAccessService.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final ArchiveInAppNotificationService inAppNotificationService = mock(ArchiveInAppNotificationService.class);
    private final ArchiveGroupOperationLogPresenter archiveGroupOperationLogPresenter = mock(ArchiveGroupOperationLogPresenter.class);
    private final OperationLogRecorder operationLogRecorder = mock(OperationLogRecorder.class);
    private final ArchiveGroupServiceImpl service = new ArchiveGroupServiceImpl(
            groupMapper, archiveConnectionMapper, taskMapper, idItemMapper, timeItemMapper,
            sysUserMapper, currentUserService, archiveResourceAccessService, dataPermissionService,
            inAppNotificationService, archiveGroupOperationLogPresenter, operationLogRecorder);

    // --- updateOwner tests ---

    @Test
    void shouldRejectNormalUserUpdateOwner() {
        CurrentUserInfo normalUser = new CurrentUserInfo();
        normalUser.setUserId(1L);
        normalUser.setRoleCode(RoleConstants.NORMAL_USER);
        when(currentUserService.getCurrentUser()).thenReturn(normalUser);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.updateOwner(1L, 2L));
        assertEquals(StarterErrorCode.OWNER_UPDATE_NOT_ALLOWED, error.getErrorCode());
    }

    @Test
    void shouldRejectUpdateOwnerWhenUserNotFound() {
        CurrentUserInfo admin = new CurrentUserInfo();
        admin.setUserId(1L);
        admin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(sysUserMapper.selectById(999L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.updateOwner(1L, 999L));
        assertEquals(StarterErrorCode.OWNER_USER_INVALID, error.getErrorCode());
    }

    @Test
    void shouldRejectUpdateOwnerWhenUserDisabled() {
        CurrentUserInfo admin = new CurrentUserInfo();
        admin.setUserId(1L);
        admin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        SysUser disabledUser = new SysUser();
        disabledUser.setId(2L);
        disabledUser.setStatus(EnableStatusEnum.DISABLED.getCode());
        when(sysUserMapper.selectById(2L)).thenReturn(disabledUser);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.updateOwner(1L, 2L));
        assertEquals(StarterErrorCode.OWNER_USER_DISABLED, error.getErrorCode());
    }

    @Test
    void shouldAllowPlatformAdminUpdateOwner() {
        CurrentUserInfo platformAdmin = new CurrentUserInfo();
        platformAdmin.setUserId(1L);
        platformAdmin.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(platformAdmin);

        SysUser enabledUser = new SysUser();
        enabledUser.setId(2L);
        enabledUser.setStatus(EnableStatusEnum.ENABLED.getCode());
        when(sysUserMapper.selectById(2L)).thenReturn(enabledUser);

        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(0);

        ArchiveGroup after = new ArchiveGroup();
        after.setId(10L);
        after.setOwnerUserId(2L);
        when(groupMapper.selectById(10L)).thenReturn(group).thenReturn(after);

        service.updateOwner(10L, 2L);

        verify(groupMapper).updateOwner(10L, 2L);
    }

    // --- create with owner validation tests ---

    @Test
    void shouldSetOwnerToSelfForNormalUserOnCreate() {
        CurrentUserInfo normalUser = new CurrentUserInfo();
        normalUser.setUserId(1L);
        normalUser.setRoleCode(RoleConstants.NORMAL_USER);
        when(currentUserService.getCurrentUser()).thenReturn(normalUser);
        stubEnabledDatasources();

        ArchiveGroup group = new ArchiveGroup();
        group.setGroupCode("TEST_GROUP");
        group.setGroupName("Test Group");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setOwnerUserId(999L); // attempt to set other user

        // Mock owner validation (called in validateOwnerOnCreate before forcing to self)
        SysUser ownerUser = new SysUser();
        ownerUser.setId(999L);
        ownerUser.setStatus(EnableStatusEnum.ENABLED.getCode());
        when(sysUserMapper.selectById(999L)).thenReturn(ownerUser);

        service.create(group);

        assertEquals(1L, group.getOwnerUserId()); // should be forced to self
        verify(groupMapper).insert(group);
    }

    @Test
    void shouldRejectArchiveAdminSetOwnerToNonCreatedUser() {
        CurrentUserInfo archiveAdmin = new CurrentUserInfo();
        archiveAdmin.setUserId(1L);
        archiveAdmin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(archiveAdmin);
        stubEnabledDatasources();

        SysUser otherUser = new SysUser();
        otherUser.setId(2L);
        otherUser.setStatus(EnableStatusEnum.ENABLED.getCode());
        otherUser.setCreatorId("999"); // created by another user
        when(sysUserMapper.selectById(2L)).thenReturn(otherUser);

        ArchiveGroup group = new ArchiveGroup();
        group.setGroupCode("TEST_GROUP");
        group.setGroupName("Test Group");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setOwnerUserId(2L);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.create(group));
        assertEquals(StarterErrorCode.OWNER_USER_NOT_CREATED_BY_YOU, error.getErrorCode());
    }

    @Test
    void shouldAllowArchiveAdminSetOwnerToSelfCreatedUser() {
        CurrentUserInfo archiveAdmin = new CurrentUserInfo();
        archiveAdmin.setUserId(1L);
        archiveAdmin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(archiveAdmin);
        stubEnabledDatasources();

        SysUser createdUser = new SysUser();
        createdUser.setId(2L);
        createdUser.setStatus(EnableStatusEnum.ENABLED.getCode());
        createdUser.setCreatorId("1"); // created by current archive admin
        when(sysUserMapper.selectById(2L)).thenReturn(createdUser);

        ArchiveGroup group = new ArchiveGroup();
        group.setGroupCode("TEST_GROUP");
        group.setGroupName("Test Group");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setOwnerUserId(2L);

        service.create(group);

        assertEquals(2L, group.getOwnerUserId());
        verify(groupMapper).insert(group);
    }

    @Test
    void shouldSetOwnerToSelfForArchiveAdminWhenOwnerNotSpecified() {
        CurrentUserInfo archiveAdmin = new CurrentUserInfo();
        archiveAdmin.setUserId(1L);
        archiveAdmin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(archiveAdmin);
        stubEnabledDatasources();

        ArchiveGroup group = new ArchiveGroup();
        group.setGroupCode("TEST_GROUP");
        group.setGroupName("Test Group");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setOwnerUserId(null); // no owner specified

        service.create(group);

        assertEquals(1L, group.getOwnerUserId());
        verify(groupMapper).insert(group);
    }

    @Test
    void shouldAllowPlatformAdminSpecifyAnyOwner() {
        CurrentUserInfo platformAdmin = new CurrentUserInfo();
        platformAdmin.setUserId(1L);
        platformAdmin.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(platformAdmin);
        stubEnabledDatasources();

        SysUser anyUser = new SysUser();
        anyUser.setId(5L);
        anyUser.setStatus(EnableStatusEnum.ENABLED.getCode());
        when(sysUserMapper.selectById(5L)).thenReturn(anyUser);

        ArchiveGroup group = new ArchiveGroup();
        group.setGroupCode("TEST_GROUP");
        group.setGroupName("Test Group");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setOwnerUserId(5L);

        service.create(group);

        assertEquals(5L, group.getOwnerUserId());
        verify(groupMapper).insert(group);
    }

    // --- pagination tests ---

    @Test
    void shouldReturnPaginatedGroupsForAdmin() {
        CurrentUserInfo admin = new CurrentUserInfo();
        admin.setUserId(1L);
        admin.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        ArchiveGroup group1 = new ArchiveGroup();
        group1.setId(1L);
        ArchiveGroup group2 = new ArchiveGroup();
        group2.setId(2L);

        when(groupMapper.countByKeyword(null, null, null)).thenReturn(10);
        when(groupMapper.selectByKeyword(null, null, null, 0, 2)).thenReturn(Arrays.asList(group1, group2));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Collections.emptyList());

        PageResult<ArchiveGroupView> result = service.findPage(null, 1, 2, null, null);

        assertEquals(2, result.getData().size());
        assertEquals(10, result.getTotal());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getSize());
    }

    @Test
    void shouldReturnPaginatedGroupsForNormalUser() {
        CurrentUserInfo normalUser = new CurrentUserInfo();
        normalUser.setUserId(1L);
        normalUser.setRoleCode(RoleConstants.NORMAL_USER);
        when(currentUserService.getCurrentUser()).thenReturn(normalUser);

        ArchiveGroup ownGroup = new ArchiveGroup();
        ownGroup.setId(1L);
        ownGroup.setOwnerUserId(1L);

        when(groupMapper.countByOwner(1L, null)).thenReturn(5);
        when(groupMapper.selectPageByOwner(1L, null, 0, 10)).thenReturn(Collections.singletonList(ownGroup));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Collections.emptyList());

        PageResult<ArchiveGroupView> result = service.findPage(null, 1, 10, null, null);

        assertEquals(1, result.getData().size());
        assertEquals(5, result.getTotal());
    }

    @Test
    void shouldReturnPaginatedGroupsForArchiveAdmin() {
        CurrentUserInfo archiveAdmin = new CurrentUserInfo();
        archiveAdmin.setUserId(2L);
        archiveAdmin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(archiveAdmin);

        ArchiveGroup authorizedGroup = new ArchiveGroup();
        authorizedGroup.setId(3L);

        when(groupMapper.countAuthorized(2L, null)).thenReturn(3);
        when(groupMapper.selectAuthorizedPage(2L, null, 0, 10)).thenReturn(Collections.singletonList(authorizedGroup));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Collections.emptyList());

        PageResult<ArchiveGroupView> result = service.findPage(null, 1, 10, null, null);

        assertEquals(1, result.getData().size());
        assertEquals(3, result.getTotal());
        assertEquals(3L, result.getData().get(0).getId());
    }

    @Test
    void shouldReturnEmptyPageWhenNoGroups() {
        CurrentUserInfo admin = new CurrentUserInfo();
        admin.setUserId(1L);
        admin.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        when(groupMapper.countByKeyword(null, null, null)).thenReturn(0);
        when(groupMapper.selectByKeyword(null, null, null, 0, 10)).thenReturn(Collections.emptyList());

        PageResult<ArchiveGroupView> result = service.findPage(null, 1, 10, null, null);

        assertEquals(0, result.getData().size());
        assertEquals(0, result.getTotal());
    }

    // --- findAll query filtering tests ---

    @Test
    void shouldReturnAllGroupsForAdmin() {
        CurrentUserInfo admin = new CurrentUserInfo();
        admin.setUserId(1L);
        admin.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(admin);

        ArchiveGroup group1 = new ArchiveGroup();
        group1.setId(1L);
        ArchiveGroup group2 = new ArchiveGroup();
        group2.setId(2L);
        when(groupMapper.selectList(null)).thenReturn(Arrays.asList(group1, group2));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Collections.emptyList());

        List<ArchiveGroupView> result = service.findAll(null);

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnOnlyOwnGroupsForNormalUser() {
        CurrentUserInfo normalUser = new CurrentUserInfo();
        normalUser.setUserId(1L);
        normalUser.setRoleCode(RoleConstants.NORMAL_USER);
        when(currentUserService.getCurrentUser()).thenReturn(normalUser);

        ArchiveGroup ownGroup = new ArchiveGroup();
        ownGroup.setId(1L);
        ownGroup.setOwnerUserId(1L);
        ArchiveGroup otherGroup = new ArchiveGroup();
        otherGroup.setId(2L);
        otherGroup.setOwnerUserId(2L);

        when(groupMapper.selectAuthorizedList(1L, null)).thenReturn(Arrays.asList(ownGroup, otherGroup));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Collections.emptyList());

        List<ArchiveGroupView> result = service.findAll(null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void shouldReturnAuthorizedDatasourceGroupsForArchiveAdmin() {
        CurrentUserInfo archiveAdmin = new CurrentUserInfo();
        archiveAdmin.setUserId(1L);
        archiveAdmin.setRoleCode(RoleConstants.ARCHIVE_ADMIN);
        when(currentUserService.getCurrentUser()).thenReturn(archiveAdmin);
        when(dataPermissionService.getAuthorizedDatasourceIds(1L))
                .thenReturn(new HashSet<>(Collections.singletonList(1L)));

        ArchiveGroup authorizedGroup = new ArchiveGroup();
        authorizedGroup.setId(1L);
        authorizedGroup.setSourceDatasourceId(1L);
        ArchiveGroup unauthorizedGroup = new ArchiveGroup();
        unauthorizedGroup.setId(2L);
        unauthorizedGroup.setSourceDatasourceId(2L);

        when(groupMapper.selectAuthorizedList(1L, null))
                .thenReturn(Arrays.asList(authorizedGroup, unauthorizedGroup));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Collections.emptyList());

        List<ArchiveGroupView> result = service.findAll(null);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    private void stubEnabledDatasources() {
        ArchiveConnection src = new ArchiveConnection();
        src.setId(1L);
        src.setStatus(1);
        ArchiveConnection tgt = new ArchiveConnection();
        tgt.setId(2L);
        tgt.setStatus(1);
        when(archiveConnectionMapper.selectById(1L)).thenReturn(src);
        when(archiveConnectionMapper.selectById(2L)).thenReturn(tgt);
    }
}
