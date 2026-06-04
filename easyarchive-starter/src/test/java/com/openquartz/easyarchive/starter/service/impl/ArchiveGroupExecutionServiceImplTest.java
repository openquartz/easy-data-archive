package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.support.ArchiveGroupTaskDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveGroupExecutionServiceImplTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveGroupItemByIdMapper idMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveConnectionMapper datasourceMapper = mock(ArchiveConnectionMapper.class);
    private final ArchiveGroupTaskDispatcher dispatcher = mock(ArchiveGroupTaskDispatcher.class);
    private final ArchiveTaskLogService taskLogService = mock(ArchiveTaskLogService.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final ArchiveGroupExecutionServiceImpl service = new ArchiveGroupExecutionServiceImpl(
            groupMapper, idMapper, timeMapper, taskMapper, datasourceMapper, dispatcher, taskLogService,
            dataPermissionService);

    @Test
    void shouldRejectTriggerWhenActiveTaskExists() {
        ArchiveGroup group = enabledGroup();
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.trigger(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_HAS_ACTIVE_TASK, error.getErrorCode());
        verify(taskMapper, never()).insert(any());
        verify(dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void shouldRejectTriggerWhenNoEnabledItems() {
        ArchiveGroup group = enabledGroup();
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(0);
        when(idMapper.countEnabledByGroupId(10L)).thenReturn(0);
        when(timeMapper.countEnabledByGroupId(10L)).thenReturn(0);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.trigger(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_HAS_NO_ENABLED_ITEM, error.getErrorCode());
        verify(taskMapper, never()).insert(any());
        verify(dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldCreateWaitingTaskAndDispatch() {
        ArchiveGroup group = enabledGroup();
        Object source = datasource(1L, "SRC", "jdbc:mysql://source");
        Object target = datasource(2L, "DST", "jdbc:mysql://target");
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(group);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(0);
        when(idMapper.countEnabledByGroupId(10L)).thenReturn(1);
        when(timeMapper.countEnabledByGroupId(10L)).thenReturn(0);
        doReturn(source).when(datasourceMapper).selectById(1L);
        doReturn(target).when(datasourceMapper).selectById(2L);

        ArchiveGroupExecuteTask task = service.trigger(10L);

        assertSame(task, captureInsertedTask());
        assertEquals(10L, task.getGroupId());
        assertEquals(ArchiveGroupExecuteTask.STATUS_WAITING, task.getExecuteStatus());
        assertEquals(0L, task.getProcessedRecords());
        assertEquals(0L, task.getFinishedFlag());
        assertNotNull(task.getStartTime());

        ArgumentCaptor<ArchiveRuleLoader> loaderCaptor = ArgumentCaptor.forClass(ArchiveRuleLoader.class);
        ArgumentCaptor<Pair> connectionCaptor = ArgumentCaptor.forClass(Pair.class);
        verify(dispatcher).dispatch(loaderCaptor.capture(), same(task), connectionCaptor.capture());
        assertNotNull(loaderCaptor.getValue());
        ArchiveConnection dispatchedSource = (ArchiveConnection) connectionCaptor.getValue().getKey();
        ArchiveConnection dispatchedTarget = (ArchiveConnection) connectionCaptor.getValue().getValue();
        assertEquals("SRC", dispatchedSource.getConnectCode());
        assertEquals("jdbc:mysql://source", dispatchedSource.getUrl());
        assertEquals("DST", dispatchedTarget.getConnectCode());
        assertEquals("jdbc:mysql://target", dispatchedTarget.getUrl());
    }

    @Test
    void shouldCreateWaitingTaskWhenTriggeredByGroupCode() {
        ArchiveGroup group = enabledGroup();
        Object source = datasource(1L, "SRC", "jdbc:mysql://source");
        Object target = datasource(2L, "DST", "jdbc:mysql://target");
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(group);
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(0);
        when(idMapper.countEnabledByGroupId(10L)).thenReturn(1);
        when(timeMapper.countEnabledByGroupId(10L)).thenReturn(0);
        doReturn(source).when(datasourceMapper).selectById(1L);
        doReturn(target).when(datasourceMapper).selectById(2L);

        ArchiveGroupExecuteTask task = service.trigger("ORDER_ARCHIVE");

        assertEquals(10L, task.getGroupId());
        assertEquals(ArchiveGroupExecuteTask.STATUS_WAITING, task.getExecuteStatus());
        verify(groupMapper).selectByCode("ORDER_ARCHIVE");
    }

    @Test
    void shouldRejectTriggerWhenGroupCodeIsBlank() {
        StarterManageException error = assertThrows(StarterManageException.class, () -> service.trigger("  "));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_CODE_REQUIRED, error.getErrorCode());
        verify(groupMapper, never()).selectByCode(any());
    }

    @Test
    void shouldRejectDisabledGroup() {
        ArchiveGroup group = enabledGroup();
        group.setEnableStatus(1);
        when(groupMapper.selectById(10L)).thenReturn(group);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.trigger(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_DISABLED, error.getErrorCode());
        verify(taskMapper, never()).insert(any());
        verify(dispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void shouldCancelLatestActiveTaskForGroup() {
        ArchiveGroup group = enabledGroup();
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(88L);
        task.setGroupId(10L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(taskMapper.selectLatestActiveByGroupId(10L)).thenReturn(task);

        ArchiveGroupExecuteTask cancelled = service.cancelActiveTask(10L, "user request");

        assertSame(task, cancelled);
        verify(taskLogService).cancelTask(88L, "user request");
    }

    @Test
    void shouldRejectGroupCancelWhenNoActiveTaskExists() {
        ArchiveGroup group = enabledGroup();
        when(groupMapper.selectById(10L)).thenReturn(group);
        when(taskMapper.selectLatestActiveByGroupId(10L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.cancelActiveTask(10L, "user request"));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_HAS_NO_ACTIVE_TASK, error.getErrorCode());
        verify(taskLogService, never()).cancelTask(any(), anyString());
    }

    private ArchiveGroupExecuteTask captureInsertedTask() {
        ArgumentCaptor<ArchiveGroupExecuteTask> taskCaptor = ArgumentCaptor.forClass(ArchiveGroupExecuteTask.class);
        verify(taskMapper).insert(taskCaptor.capture());
        return taskCaptor.getValue();
    }

    private static ArchiveGroup enabledGroup() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setEnableStatus(0);
        return group;
    }

    private static Object datasource(Long id, String code, String jdbcUrl) {
        try {
            Class<?> returnType = ArchiveConnectionMapper.class.getMethod("selectById", Long.class).getReturnType();
            Object datasource = returnType.getDeclaredConstructor().newInstance();
            invokeSetter(datasource, "setId", Long.class, id);
            invokeSetter(datasource, "setDatasourceCode", String.class, code);
            invokeSetter(datasource, "setDatasourceName", String.class, code + " datasource");
            invokeSetter(datasource, "setDatasourceType", String.class, "MYSQL");
            invokeSetter(datasource, "setJdbcUrl", String.class, jdbcUrl);
            invokeSetter(datasource, "setUsername", String.class, "root");
            invokeSetter(datasource, "setPasswordCipher", String.class, "secret");
            invokeSetter(datasource, "setStatus", Integer.class, 1);
            return datasource;
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static void invokeSetter(Object target, String methodName, Class<?> parameterType, Object value)
            throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName, parameterType);
        method.invoke(target, value);
    }
}
