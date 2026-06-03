package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.DatasourceOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveConnectionServiceImplTest {

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
