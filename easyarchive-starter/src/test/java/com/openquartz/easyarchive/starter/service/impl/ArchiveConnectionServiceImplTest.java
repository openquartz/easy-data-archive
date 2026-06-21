package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.common.util.CryptoUtil;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import com.openquartz.easyarchive.starter.support.DatasourceConnectionTester;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

    private String encryptPassword(String plaintext) {
        return CryptoUtil.encrypt(plaintext);
    }

    private void setupAdminMock(CurrentUserService userService) {
        CurrentUserInfo currentUser = mock(CurrentUserInfo.class);
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(currentUser.getRoleCode()).thenReturn("ADMIN");
    }

    @Test
    void shouldFindDatasourceByConnectionCode() {
        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(1L);
        datasource.setDatasourceCode("mysql_archive");
        datasource.setPasswordCipher(encryptPassword("found-pass"));

        when(mapper.selectByCode("mysql_archive")).thenReturn(datasource);

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);

        ArchiveConnection result = service.getByConnectionCode("mysql_archive");

        // Password should be decrypted after retrieval
        assertSame(datasource, result);
        assertEquals("found-pass", result.getPasswordCipher());
        verify(mapper).selectByCode("mysql_archive");
    }

    @Test
    void shouldResetStatusToUntestedWhenJdbcConfigChanges() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setJdbcUrl("jdbc:mysql://old");
        before.setUsername("archive");
        before.setPasswordCipher(encryptPassword("secret"));
        before.setStatus(STATUS_ENABLED);

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setJdbcUrl("jdbc:mysql://new");
        after.setUsername("archive");
        after.setPasswordCipher(encryptPassword("secret"));
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
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        ArchiveConnection updated = service.update(input);

        assertSame(after, updated);
        assertEquals(STATUS_UNTESTED, input.getStatus());
        assertEquals("secret", updated.getPasswordCipher());
        verify(mapper).update(argThat(saved -> {
            // Stored password should be encrypted
            return saved.getPasswordCipher() != null
                    && saved.getPasswordCipher().contains(".")
                    && !saved.getPasswordCipher().equals("secret");
        }));
    }

    @Test
    void shouldKeepStatusWhenOnlyDisplayFieldsChange() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setJdbcUrl("jdbc:mysql://same");
        before.setUsername("archive");
        before.setPasswordCipher(encryptPassword("secret"));
        before.setStatus(STATUS_ENABLED);

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setDatasourceName("新名称");
        after.setJdbcUrl("jdbc:mysql://same");
        after.setUsername("archive");
        after.setPasswordCipher(encryptPassword("secret"));
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
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        service.update(input);

        assertEquals(STATUS_ENABLED, input.getStatus());
        verify(mapper).update(input);
    }

    @Test
    void shouldEnableDatasourceAfterSuccessfulConnectionTest() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceConnectionTester tester = mock(DatasourceConnectionTester.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection persisted = new ArchiveConnection();
        persisted.setId(1L);
        persisted.setDatasourceCode("mysql_archive");
        persisted.setJdbcUrl("jdbc:mysql://same");
        persisted.setUsername("archive");
        persisted.setPasswordCipher(encryptPassword("secret"));
        persisted.setStatus(STATUS_UNTESTED);

        when(mapper.selectById(1L)).thenReturn(persisted);
        when(tester.testConnection(any())).thenReturn(true);
        when(presenter.buildTestConnection(persisted, true))
                .thenReturn(new OperationLogCommand("DATASOURCE", "TEST", "测试连接", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, tester, currentUserService, datasourceAuthorizationService, presenter, recorder);

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
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceConnectionTester tester = mock(DatasourceConnectionTester.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection persisted = new ArchiveConnection();
        persisted.setId(1L);
        persisted.setDatasourceCode("mysql_archive");
        persisted.setJdbcUrl("jdbc:mysql://same");
        persisted.setUsername("archive");
        persisted.setPasswordCipher(encryptPassword("secret"));
        persisted.setStatus(STATUS_DISABLED);

        when(mapper.selectById(1L)).thenReturn(persisted);
        when(tester.testConnection(any())).thenReturn(false);
        when(presenter.buildTestConnection(persisted, false))
                .thenReturn(new OperationLogCommand("DATASOURCE", "TEST", "测试连接", "DATASOURCE",
                        1L, "mysql_archive", "content", null));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, tester, currentUserService, datasourceAuthorizationService, presenter, recorder);

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
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection persisted = new ArchiveConnection();
        persisted.setId(1L);
        persisted.setStatus(STATUS_UNTESTED);
        when(mapper.selectById(1L)).thenReturn(persisted);

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);

        StarterManageException error = assertThrows(StarterManageException.class,
                () -> service.updateStatus(1L, STATUS_ENABLED));
        assertEquals(StarterErrorCode.DATASOURCE_STATUS_MANUAL_UPDATE_UNSUPPORTED, error.getErrorCode());
        verify(mapper, never()).update(any());
    }

    @Test
    void shouldRecordUpdateOperationAfterLoadingBeforeState() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
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
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        service.update(input);

        verify(currentUserService).assertAdmin();
        verify(mapper, times(2)).selectById(1L);
        verify(mapper).update(input);
        verify(recorder).record(any());
    }

    @Test
    void create_shouldStoreEncryptedPassword() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(1L);
        datasource.setDatasourceCode("mysql_archive");
        datasource.setJdbcUrl("jdbc:mysql://host:3306/db");
        datasource.setUsername("archive");
        datasource.setPasswordCipher("plaintext-secret");

        when(mapper.selectById(any())).thenThrow(new RuntimeException("no-op"));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        service.create(datasource);

        verify(mapper).insert(argThat(saved -> {
            // The stored password must be encrypted (contain '.')
            return saved.getPasswordCipher() != null
                    && saved.getPasswordCipher().contains(".");
        }));
    }

    @Test
    void create_withEmptyPassword_keepsEmpty() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(1L);
        datasource.setDatasourceCode("mysql_archive");
        datasource.setJdbcUrl("jdbc:mysql://host:3306/db");
        datasource.setUsername("archive");
        datasource.setPasswordCipher("");

        when(mapper.selectById(any())).thenThrow(new RuntimeException("no-op"));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        service.create(datasource);

        verify(mapper).insert(argThat(saved -> {
            // Empty password should remain empty (not encrypted)
            return saved.getPasswordCipher().isEmpty();
        }));
    }

    @Test
    void findById_shouldMaskPasswordForApiResponse() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(1L);
        datasource.setDatasourceCode("mysql_archive");
        String encryptedPassword = encryptPassword("found-password");
        datasource.setPasswordCipher(encryptedPassword);

        when(mapper.selectById(1L)).thenReturn(datasource);

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        ArchiveConnection result = service.findById(1L);

        assertNotNull(result);
        assertEquals("****", result.getPasswordCipher());
    }

    @Test
    void findAll_shouldMaskPasswordsForApiResponse() {
        CurrentUserService currentUserService = mock(CurrentUserService.class);
        setupAdminMock(currentUserService);

        ArchiveConnectionMapper mapper = mock(ArchiveConnectionMapper.class);
        DatasourceAuthorizationService datasourceAuthorizationService = mock(DatasourceAuthorizationService.class);
        DatasourceOperationLogPresenter presenter = mock(DatasourceOperationLogPresenter.class);
        OperationLogRecorder recorder = mock(OperationLogRecorder.class);

        ArchiveConnection ds1 = new ArchiveConnection();
        ds1.setId(1L);
        ds1.setDatasourceCode("ds1");
        ds1.setPasswordCipher(encryptPassword("pass1"));

        ArchiveConnection ds2 = new ArchiveConnection();
        ds2.setId(2L);
        ds2.setDatasourceCode("ds2");
        ds2.setPasswordCipher(encryptPassword("pass2"));

        when(mapper.selectList(any(), any())).thenReturn(java.util.List.of(ds1, ds2));

        ArchiveConnectionServiceImpl service =
                new ArchiveConnectionServiceImpl(mapper, null, currentUserService, datasourceAuthorizationService, presenter, recorder);
        java.util.List<ArchiveConnection> results = service.findAll();

        assertEquals(2, results.size());
        assertEquals("****", results.get(0).getPasswordCipher());
        assertEquals("****", results.get(1).getPasswordCipher());
    }
}
