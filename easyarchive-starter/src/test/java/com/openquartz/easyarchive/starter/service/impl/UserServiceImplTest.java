package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.model.PlatformCapabilityEnum;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.RoleCapabilityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

class UserServiceImplTest {

    private final SysUserMapper userMapper = mock(SysUserMapper.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final RoleCapabilityService roleCapabilityService = mock(RoleCapabilityService.class);
    private final UserOperationLogPresenter presenter = mock(UserOperationLogPresenter.class);
    private final OperationLogRecorder recorder = mock(OperationLogRecorder.class);
    private final UserServiceImpl service = new UserServiceImpl(
            userMapper, passwordEncoder, currentUserService, roleCapabilityService, presenter, recorder);

    @Test
    void shouldEncodePasswordAndRecordCreateOperation() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("alice");
        user.setPassword("plain");
        user.setRoleCode("platform_admin");
        when(passwordEncoder.encode("plain")).thenReturn("encoded");
        when(presenter.buildCreate(any())).thenReturn(command("CREATE"));
        doNothing().when(currentUserService).assertAdmin();
        when(currentUserService.getCurrentUser()).thenReturn(platformAdmin());

        service.create(user);

        assertEquals("encoded", user.getPassword());
        verify(userMapper).insert(user);
        verify(recorder).record(any());
    }

    @Test
    void shouldLoadBeforeAndAfterForUpdateOperationLog() {
        SysUser before = user(2L, "bob", 0);
        before.setRealName("Bob");

        SysUser after = user(2L, "bob", 1);
        after.setRealName("Bob Lee");

        SysUser input = new SysUser();
        input.setId(2L);
        input.setRealName("Bob Lee");
        input.setStatus(1);

        when(userMapper.selectById(2L)).thenReturn(before, after);
        when(presenter.buildUpdate(before, after, false)).thenReturn(command("UPDATE"));
        doNothing().when(currentUserService).assertAdmin();
        when(currentUserService.getCurrentUser()).thenReturn(platformAdmin());

        service.update(input);

        verify(userMapper, times(2)).selectById(2L);
        verify(userMapper).update(input);
        verify(recorder).record(any());
    }

    @Test
    void shouldRecordStatusUpdateAfterLoadingBeforeAndAfter() {
        SysUser before = user(3L, "cindy", 0);
        SysUser after = user(3L, "cindy", 1);
        when(userMapper.selectById(3L)).thenReturn(before, after);
        when(presenter.buildStatusUpdate(before, after)).thenReturn(command("STATUS"));
        doNothing().when(currentUserService).assertAdmin();

        service.updateStatus(3L, 1);

        verify(userMapper, times(2)).selectById(3L);
        verify(userMapper).update(any(SysUser.class));
        verify(recorder).record(any());
    }

    @Test
    void shouldNormalizeLegacyRoleCodesWhenReadingUsers() {
        SysUser admin = user(1L, "admin", 0);
        admin.setRoleCode("ADMIN");
        SysUser user = user(2L, "user", 0);
        user.setRoleCode("USER");
        when(userMapper.selectList(null)).thenReturn(Arrays.asList(admin, user));
        when(currentUserService.getCurrentUser()).thenReturn(platformAdmin());
        when(roleCapabilityService.hasCapability(eq("platform_admin"), eq(PlatformCapabilityEnum.USER_VIEW))).thenReturn(true);

        List<SysUser> result = service.findAll();

        assertEquals("platform_admin", result.get(0).getRoleCode());
        assertEquals("archive_admin", result.get(1).getRoleCode());
        assertIterableEquals(Arrays.asList(admin, user), result);
    }

    private static SysUser user(Long id, String username, Integer status) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setUsername(username);
        user.setStatus(status);
        return user;
    }

    private static OperationLogCommand command(String action) {
        return new OperationLogCommand("USER", action, action, "USER", 1L, "alice", action, null);
    }

    private static CurrentUserInfo platformAdmin() {
        CurrentUserInfo info = new CurrentUserInfo();
        info.setUserId(1L);
        info.setUsername("admin");
        info.setRoleCode("platform_admin");
        return info;
    }
}
