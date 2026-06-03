package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationValueFormatter;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
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

    public OperationLogCommand buildIdCreate(ArchiveGroupItemById item) {
        String content = "新增按ID分组项：\"来源表\" 为 \"" + item.getSourceTable()
                + "\"; \"目标表\" 为 \"" + item.getTargetTable() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_ID", "CREATE", "新增分组项", "ARCHIVE_GROUP_ITEM_ID",
                item.getId(), item.getSourceTable(), content, Collections.emptyList());
    }

    public OperationLogCommand buildIdUpdate(ArchiveGroupItemById before, ArchiveGroupItemById after) {
        String content = "\"目标表\" 从 \"" + before.getTargetTable() + "\" 修改为：\"" + after.getTargetTable()
                + "\"; \"步长\" 从 \"" + before.getStepCount() + "\" 修改为：\"" + after.getStepCount() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_ID", "UPDATE", "编辑分组项", "ARCHIVE_GROUP_ITEM_ID",
                after.getId(), after.getSourceTable(), content, Collections.emptyList());
    }

    public OperationLogCommand buildIdDelete(ArchiveGroupItemById before) {
        String content = "删除按ID分组项：\"来源表\" 为 \"" + before.getSourceTable()
                + "\"; \"目标表\" 为 \"" + before.getTargetTable() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_ID", "DELETE", "删除分组项", "ARCHIVE_GROUP_ITEM_ID",
                before.getId(), before.getSourceTable(), content, Collections.emptyList());
    }

    public OperationLogCommand buildTimeCreate(ArchiveGroupItemByTime item) {
        String content = "新增按时间分组项：\"来源表\" 为 \"" + item.getSourceTable()
                + "\"; \"目标表\" 为 \"" + item.getTargetTable() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_TIME", "CREATE", "新增分组项", "ARCHIVE_GROUP_ITEM_TIME",
                item.getId(), item.getSourceTable(), content, Collections.emptyList());
    }

    public OperationLogCommand buildTimeUpdate(ArchiveGroupItemByTime before, ArchiveGroupItemByTime after) {
        String content = "\"目标表\" 从 \"" + before.getTargetTable() + "\" 修改为：\"" + after.getTargetTable()
                + "\"; \"步长\" 从 \"" + before.getStepCount() + "\" 修改为：\"" + after.getStepCount() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_TIME", "UPDATE", "编辑分组项", "ARCHIVE_GROUP_ITEM_TIME",
                after.getId(), after.getSourceTable(), content, Collections.emptyList());
    }

    public OperationLogCommand buildTimeStatusUpdate(ArchiveGroupItemByTime before, ArchiveGroupItemByTime after) {
        String buttonName = after.getEnableStatus() != null && after.getEnableStatus() == 0 ? "启用分组项" : "停用分组项";
        String content = "执行“" + buttonName + "”操作，将 \"启用状态\" 从 \""
                + formatter.formatEnableStatus(before.getEnableStatus()) + "\" 修改为：\""
                + formatter.formatEnableStatus(after.getEnableStatus()) + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_TIME", "STATUS", buttonName, "ARCHIVE_GROUP_ITEM_TIME",
                after.getId(), after.getSourceTable(), content, Collections.emptyList());
    }

    public OperationLogCommand buildTimeDelete(ArchiveGroupItemByTime before) {
        String content = "删除按时间分组项：\"来源表\" 为 \"" + before.getSourceTable()
                + "\"; \"目标表\" 为 \"" + before.getTargetTable() + "\"";
        return new OperationLogCommand("ARCHIVE_GROUP_ITEM_TIME", "DELETE", "删除分组项", "ARCHIVE_GROUP_ITEM_TIME",
                before.getId(), before.getSourceTable(), content, Collections.emptyList());
    }
}
