package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatasourceOperationLogPresenterTest {

    @Test
    void shouldRenderUpdateContentWithChineseLabelsAndMaskedPassword() {
        ArchiveConnection before = new ArchiveConnection();
        before.setId(1L);
        before.setDatasourceCode("mysql_archive");
        before.setDatasourceName("归档库");
        before.setPassword("cipher-old");
        before.setStatus(1);

        ArchiveConnection after = new ArchiveConnection();
        after.setId(1L);
        after.setDatasourceCode("mysql_archive");
        after.setDatasourceName("归档库华东");
        after.setPassword("cipher-new");
        after.setStatus(3);

        DatasourceOperationLogPresenter presenter = new DatasourceOperationLogPresenter();
        OperationLogCommand command = presenter.buildUpdate(before, after);

        assertEquals("编辑数据源", command.getButtonName());
        assertEquals("\"数据源名称\" 从 \"归档库\" 修改为：\"归档库华东\"; \"状态\" 从 \"正常\" 修改为：\"禁用\"; \"密码\" 已更新",
                command.getContent());
    }
}
