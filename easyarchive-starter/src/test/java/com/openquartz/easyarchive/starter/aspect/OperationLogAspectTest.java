package com.openquartz.easyarchive.starter.aspect;

import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.mapper.SysOperationLogMapper;
import com.openquartz.easyarchive.starter.model.entity.SysOperationLog;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogAspectTest {

    @Test
    void shouldPersistSuccessfulOperationLogRow() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);

        CurrentUserInfo currentUser = new CurrentUserInfo();
        currentUser.setUserId(99L);
        when(permissionService.getCurrentUser()).thenReturn(currentUser);

        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/v1/archive/groups/10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            OperationLogAspect aspect = new OperationLogAspect(mapper, permissionService);
            AspectJProxyFactory factory = new AspectJProxyFactory(new TestControllerTarget());
            factory.addAspect(aspect);
            TestControllerTarget proxy = factory.getProxy();

            proxy.update();

            ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
            verify(mapper).insert(captor.capture());
            assertEquals(99L, captor.getValue().getUserId());
            assertEquals("ARCHIVE_GROUP", captor.getValue().getModuleCode());
            assertEquals("保存分组", captor.getValue().getButtonName());
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    @Test
    void shouldTruncateOversizedErrorMessageBeforePersistingFailureLog() {
        SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/archive/groups");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        try {
            OperationLogAspect aspect = new OperationLogAspect(mapper, permissionService);
            AspectJProxyFactory factory = new AspectJProxyFactory(new FailingControllerTarget());
            factory.addAspect(aspect);
            FailingControllerTarget proxy = factory.getProxy();

            try {
                proxy.create();
            } catch (IllegalStateException expected) {
                assertEquals(longMessage().length(), expected.getMessage().length());
            }

            ArgumentCaptor<SysOperationLog> captor = ArgumentCaptor.forClass(SysOperationLog.class);
            verify(mapper).insert(captor.capture());
            assertEquals(500, captor.getValue().getErrorMessage().length());
            assertTrue(longMessage().startsWith(captor.getValue().getErrorMessage()));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }

    static class TestControllerTarget {
        @OperationLog(value = "保存分组", module = "ARCHIVE_GROUP", action = "UPDATE")
        public void update() {
        }
    }

    static class FailingControllerTarget {
        @OperationLog(value = "新增分组", module = "ARCHIVE_GROUP", action = "CREATE")
        public void create() {
            throw new IllegalStateException(longMessage());
        }
    }

    private static String longMessage() {
        return "x".repeat(700);
    }
}
