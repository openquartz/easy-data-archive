package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationValueFormatter;

import java.util.Collections;

public class ArchiveGroupItemOperationLogPresenter {

    private final OperationValueFormatter formatter = new OperationValueFormatter();

    public OperationLogCommand buildIdStatusUpdate(ArchiveGroupItemById before, ArchiveGroupItemById after) {
        String buttonName = after.getEnableStatus() != null && after.getEnableStatus() == 0 ? "启用分组项" : "停用分组项";
        String content = "执行“" + buttonName + "”操作，将 \"启用状态\" 从 \""
                + formatter.formatEnableStatus(before.getEnableStatus()) + "\" 修改为：\""
                + formatter.formatEnableStatus(after.getEnableStatus()) + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_ID", "STATUS", buttonName, "ARCHIVE_GROUP_ITEM_ID",
                after.getId(), after.getSourceTable(), content, Collections.emptyList());
    }
}
