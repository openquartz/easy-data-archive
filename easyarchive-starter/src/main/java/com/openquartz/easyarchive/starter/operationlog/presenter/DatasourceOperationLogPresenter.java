package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationValueFormatter;

import java.util.Collections;

public class DatasourceOperationLogPresenter {

    private final OperationValueFormatter formatter = new OperationValueFormatter();

    public OperationLogCommand buildUpdate(ArchiveConnection before, ArchiveConnection after) {
        String content = "\"数据源名称\" 从 \"" + before.getDatasourceName() + "\" 修改为：\"" + after.getDatasourceName() + "\""
                + "; \"状态\" 从 \"" + formatter.formatDatasourceStatus(before.getStatus()) + "\" 修改为：\""
                + formatter.formatDatasourceStatus(after.getStatus()) + "\""
                + "; " + formatter.changedPasswordText();
        return new OperationLogCommand("DATASOURCE", "UPDATE", "编辑数据源", "DATASOURCE",
                after.getId(), after.getDatasourceCode(), content, Collections.emptyList());
    }
}
