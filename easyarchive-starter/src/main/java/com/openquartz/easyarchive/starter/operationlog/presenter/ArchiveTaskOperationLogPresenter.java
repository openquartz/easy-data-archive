package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.function.Function;

@Component
public class ArchiveTaskOperationLogPresenter {

    private final Function<Long, String> groupIdentifierResolver;

    public ArchiveTaskOperationLogPresenter(ArchiveGroupMapper archiveGroupMapper) {
        this(groupId -> {
            if (groupId == null) {
                return "";
            }
            ArchiveGroup group = archiveGroupMapper.selectById(groupId);
            if (group == null) {
                return String.valueOf(groupId);
            }
            if (StringUtils.hasText(group.getGroupCode())) {
                return group.getGroupCode();
            }
            if (StringUtils.hasText(group.getGroupName())) {
                return group.getGroupName();
            }
            return String.valueOf(groupId);
        });
    }

    public ArchiveTaskOperationLogPresenter(Function<Long, String> groupIdentifierResolver) {
        this.groupIdentifierResolver = groupIdentifierResolver;
    }

    public OperationLogCommand buildCancel(ArchiveGroupExecuteTask task, String cancelReason, String result) {
        String content = "取消归档任务：\"任务ID\" 为 \"" + task.getId()
                + "\"; \"分组标识\" 为 \"" + groupIdentifierResolver.apply(task.getGroupId())
                + "\"; \"取消原因\" 为 \"" + normalize(cancelReason)
                + "\"; \"处理结果\" 为 \"" + normalize(result) + "\"";
        return new OperationLogCommand("ARCHIVE_TASK", "CANCEL", "取消归档任务", "ARCHIVE_TASK",
                task.getId(), String.valueOf(task.getId()), content, Collections.emptyList());
    }

    public OperationLogCommand buildCleanup(int retentionDays, int deletedCount) {
        String content = "清理任务日志：\"保留天数\" 为 \"" + retentionDays
                + "\"; \"清理结果\" 为 \"删除 " + deletedCount + " 条记录\"";
        return new OperationLogCommand("ARCHIVE_TASK_LOG", "CLEANUP", "清理任务日志", "ARCHIVE_TASK_LOG",
                null, null, content, Collections.emptyList());
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value : "未填写";
    }
}
