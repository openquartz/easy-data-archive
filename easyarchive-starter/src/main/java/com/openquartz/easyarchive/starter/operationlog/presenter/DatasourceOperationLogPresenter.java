package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationValueFormatter;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
public class DatasourceOperationLogPresenter {

    private final OperationValueFormatter formatter = new OperationValueFormatter();

    public OperationLogCommand buildCreate(ArchiveConnection datasource) {
        String content = "新增数据源：\"数据源编码\" 为 \"" + datasource.getDatasourceCode()
                + "\"; \"数据源名称\" 为 \"" + datasource.getDatasourceName()
                + "\"; \"数据源类型\" 为 \"" + datasource.getDatasourceType() + "\"";
        return new OperationLogCommand("DATASOURCE", "CREATE", "新增数据源", "DATASOURCE",
                datasource.getId(), datasource.getDatasourceCode(), content, Collections.emptyList());
    }

    public OperationLogCommand buildUpdate(ArchiveConnection before, ArchiveConnection after) {
        String content = "\"数据源名称\" 从 \"" + before.getDatasourceName() + "\" 修改为：\"" + after.getDatasourceName() + "\""
                + "; \"状态\" 从 \"" + formatter.formatDatasourceStatus(before.getStatus()) + "\" 修改为：\""
                + formatter.formatDatasourceStatus(after.getStatus()) + "\""
                + "; " + formatter.changedPasswordText();
        return new OperationLogCommand("DATASOURCE", "UPDATE", "编辑数据源", "DATASOURCE",
                after.getId(), after.getDatasourceCode(), content, Collections.emptyList());
    }

    public OperationLogCommand buildStatusUpdate(ArchiveConnection before, ArchiveConnection after) {
        String content = "\"状态\" 从 \"" + formatter.formatDatasourceStatus(before.getStatus()) + "\" 修改为：\""
                + formatter.formatDatasourceStatus(after.getStatus()) + "\"";
        return new OperationLogCommand("DATASOURCE", "STATUS", "修改数据源状态", "DATASOURCE",
                after.getId(), after.getDatasourceCode(), content, Collections.emptyList());
    }

    public OperationLogCommand buildTestConnection(ArchiveConnection datasource, boolean success) {
        String content = "测试连接数据源：\"数据源编码\" 为 \"" + datasource.getDatasourceCode()
                + "\"; 结果为：\"" + (success ? "成功" : "失败") + "\"";
        return new OperationLogCommand("DATASOURCE", "TEST", "测试连接", "DATASOURCE",
                datasource.getId(), datasource.getDatasourceCode(), content, Collections.emptyList());
    }
}
