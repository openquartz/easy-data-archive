package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ManagementTransactionAnnotationTest {

    @Test
    void shouldAnnotateAllManagementWriteMethodsWithTransactionalRollbackForException() throws Exception {
        assertTransactional(ArchiveConnectionServiceImpl.class, "create", ArchiveConnection.class);
        assertTransactional(ArchiveConnectionServiceImpl.class, "update", ArchiveConnection.class);
        assertTransactional(ArchiveConnectionServiceImpl.class, "updateStatus", Long.class, Integer.class);
        assertTransactional(ArchiveConnectionServiceImpl.class, "testConnection", ArchiveConnection.class);

        assertTransactional(ArchiveGroupServiceImpl.class, "create", ArchiveGroup.class);
        assertTransactional(ArchiveGroupServiceImpl.class, "update", ArchiveGroup.class);
        assertTransactional(ArchiveGroupServiceImpl.class, "updateStatus", Long.class, Integer.class);
        assertTransactional(ArchiveGroupServiceImpl.class, "delete", Long.class);

        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "create", Long.class, ArchiveGroupItemById.class);
        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "update", Long.class, Long.class, ArchiveGroupItemById.class);
        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "updateStatus", Long.class, Long.class, Integer.class);
        assertTransactional(ArchiveGroupItemByIdServiceImpl.class, "delete", Long.class, Long.class);

        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "create", Long.class, ArchiveGroupItemByTime.class);
        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "update", Long.class, Long.class, ArchiveGroupItemByTime.class);
        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "updateStatus", Long.class, Long.class, Integer.class);
        assertTransactional(ArchiveGroupItemByTimeServiceImpl.class, "delete", Long.class, Long.class);

        assertTransactional(UserServiceImpl.class, "create", SysUser.class);
        assertTransactional(UserServiceImpl.class, "update", SysUser.class);
        assertTransactional(UserServiceImpl.class, "updateStatus", Long.class, Integer.class);

        assertTransactional(UserDatasourcePermissionServiceImpl.class, "grant", Long.class, Long.class);
        assertTransactional(UserDatasourcePermissionServiceImpl.class, "revoke", Long.class, Long.class);
        assertTransactional(UserDatasourcePermissionServiceImpl.class, "replacePermissions", Long.class, List.class);

        assertTransactional(ArchiveTaskLogServiceImpl.class, "cancelTask", Long.class, String.class);
        assertTransactional(ArchiveTaskLogServiceImpl.class, "cleanup", int.class);

        assertTransactional(ArchiveGroupExecutionServiceImpl.class, "trigger", Long.class);
        assertTransactional(ArchiveGroupExecutionServiceImpl.class, "cancelActiveTask", Long.class, String.class);
    }

    @Test
    void shouldKeepReadOnlyManagementMethodsWithoutTransactionalAnnotation() throws Exception {
        assertNotTransactional(ArchiveConnectionServiceImpl.class, "findAll");
        assertNotTransactional(ArchiveConnectionServiceImpl.class, "findById", Long.class);
        assertNotTransactional(ArchiveGroupServiceImpl.class, "findAll", Integer.class);
        assertNotTransactional(ArchiveGroupServiceImpl.class, "findById", Long.class);
        assertNotTransactional(ArchiveGroupServiceImpl.class, "findOverview", Long.class);
        assertNotTransactional(UserDatasourcePermissionServiceImpl.class, "listUserPermissions", Long.class);
        assertNotTransactional(ArchiveTaskLogServiceImpl.class, "queryTasks", int.class, int.class, String.class);
        assertNotTransactional(ArchiveGroupExecutionServiceImpl.class, "requireExistingGroup", Long.class);
    }

    private static void assertTransactional(Class<?> type, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = type.getMethod(methodName, parameterTypes);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertNotNull(annotation, () -> type.getSimpleName() + "#" + methodName + " must be transactional");
        assertArrayEquals(new Class<?>[]{Exception.class}, annotation.rollbackFor(),
                () -> type.getSimpleName() + "#" + methodName + " must roll back for Exception");
    }

    private static void assertNotTransactional(Class<?> type, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = type.getDeclaredMethod(methodName, parameterTypes);
        Transactional annotation = method.getAnnotation(Transactional.class);
        assertNull(annotation, () -> type.getSimpleName() + "#" + methodName + " must stay non-transactional");
    }
}
