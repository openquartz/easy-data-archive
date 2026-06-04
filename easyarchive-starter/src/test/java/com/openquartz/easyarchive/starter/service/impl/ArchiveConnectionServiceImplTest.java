package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.support.DatasourceConnectionTester;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveConnectionServiceImplTest {

    private static final int STATUS_UNTESTED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int STATUS_DISABLED = 2;

    @Test
    void shouldResetStatusToUntestedWhenJdbcConfigChanges() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setJdbcUrl("jdbc:mysql://old");
        before.setUsername("archive");
        before.setPasswordCipher("secret");
        before.setStatus(STATUS_ENABLED);

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setJdbcUrl("jdbc:mysql://new");
        after.setUsername("archive");
        after.setPasswordCipher("secret");
        after.setStatus(STATUS_UNTESTED);

        ArchiveConnection input = new ArchiveConnection();
        input.setId(1L);
        input.setJdbcUrl("jdbc:mysql://new");
        input.setUsername("archive");

        when(mapper.selectById(1L)).thenReturn(before, after);
        when(presenter.buildUpdate(before, after))
                .thenReturn(new OperationLogCommand("DATASOURCE", "UPDATE", "编辑数据源", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, permissionService, presenter, recorder);
        ArchiveConnection updated = service.update(input);

        assertSame(after, updated);
        assertEquals(STATUS_UNTESTED, input.getStatus());
        assertEquals("secret", input.getPasswordCipher());
        verify(mapper).update(input);
    }

    @Test
    void shouldKeepStatusWhenOnlyDisplayFieldsChange() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setJdbcUrl("jdbc:mysql://same");
        before.setUsername("archive");
        before.setPasswordCipher("secret");
        before.setStatus(STATUS_ENABLED);

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setDatasourceName("新名称");
        after.setJdbcUrl("jdbc:mysql://same");
        after.setUsername("archive");
        after.setPasswordCipher("secret");
        after.setStatus(STATUS_ENABLED);

        ArchiveConnection input = new ArchiveConnection();
        input.setId(1L);
        input.setDatasourceName("新名称");
        input.setJdbcUrl("jdbc:mysql://same");
        input.setUsername("archive");

        when(mapper.selectById(1L)).thenReturn(before, after);
        when(presenter.buildUpdate(before, after))
                .thenReturn(new OperationLogCommand("DATASOURCE", "UPDATE", "编辑数据源", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, permissionService, presenter, recorder);
        service.update(input);

        assertEquals(STATUS_ENABLED, input.getStatus());
        verify(mapper).update(input);
    }

    @Test
    void shouldEnableDatasourceAfterSuccessfulConnectionTest() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceConnectionTester tester = mock(DatasourceConnectionTester.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection persisted = new ArchiveConnection();
        persisted.setId(1L);
        persisted.setDatasourceCode("mysql_archive");
        persisted.setJdbcUrl("jdbc:mysql://same");
        persisted.setUsername("archive");
        persisted.setPasswordCipher("secret");
        persisted.setStatus(STATUS_UNTESTED);

        when(mapper.selectById(1L)).thenReturn(persisted);
        when(tester.testConnection(any())).thenReturn(true);
        when(presenter.buildTestConnection(persisted, true))
                .thenReturn(new OperationLogCommand("DATASOURCE", "TEST", "测试连接", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, tester, permissionService, presenter, recorder);

        ArchiveConnection input = new ArchiveConnection();
        input.setId(1L);
        boolean result = service.testConnection(input);

        assertTrue(result);
        verify(mapper).update(argThat(item ->
                item.getId().equals(1L)
                        && Integer.valueOf(STATUS_ENABLED).equals(item.getStatus())
                        && item.getLastCheckTime() instanceof Date));
    }

    @Test
    void shouldKeepDatasourceDisabledAfterFailedConnectionTest() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceConnectionTester tester = mock(DatasourceConnectionTester.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection persisted = new ArchiveConnection();
        persisted.setId(1L);
        persisted.setDatasourceCode("mysql_archive");
        persisted.setJdbcUrl("jdbc:mysql://same");
        persisted.setUsername("archive");
        persisted.setPasswordCipher("secret");
        persisted.setStatus(STATUS_DISABLED);

        when(mapper.selectById(1L)).thenReturn(persisted);
        when(tester.testConnection(any())).thenReturn(false);
        when(presenter.buildTestConnection(persisted, false))
                .thenReturn(new OperationLogCommand("DATASOURCE", "TEST", "测试连接", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, tester, permissionService, presenter, recorder);

        ArchiveConnection input = new ArchiveConnection();
        input.setId(1L);
        boolean result = service.testConnection(input);

        assertFalse(result);
        verify(mapper).update(argThat(item ->
                item.getId().equals(1L)
                        && item.getStatus() == null
                        && item.getLastCheckTime() instanceof Date));
    }

    @Test
    void shouldRejectManualEnableBeforeSuccessfulTest() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection persisted = new ArchiveConnection();
        persisted.setId(1L);
        persisted.setStatus(STATUS_UNTESTED);
        when(mapper.selectById(1L)).thenReturn(persisted);

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, permissionService, presenter, recorder);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.updateStatus(1L, STATUS_ENABLED));
        assertEquals(StarterErrorCode.DATASOURCE_STATUS_MANUAL_UPDATE_UNSUPPORTED, error.getErrorCode());
        verify(mapper, never()).update(any());
    }

    @Test
    void shouldRecordUpdateOperationAfterLoadingBeforeState() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DataPermissionService permissionService = mock(DataPermissionService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setDatasourceCode("mysql_archive");

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setDatasourceCode("mysql_archive");
        after.setDatasourceName("归档库华东");

        ArchiveConnection input = new ArchiveConnection();
        input.setId(1L);
        input.setDatasourceCode("mysql_archive");
        input.setDatasourceName("归档库华东");

        when(mapper.selectById(1L)).thenReturn(before, after);
        when(presenter.buildUpdate(before, after))
                .thenReturn(new OperationLogCommand("DATASOURCE", "UPDATE", "编辑数据源", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, permissionService, presenter, recorder);
        service.update(input);

        verify(permissionService).assertAdmin();
        verify(mapper, times(2)).selectById(1L);
        verify(mapper).update(input);
        verify(recorder).record(any());
    }
}
