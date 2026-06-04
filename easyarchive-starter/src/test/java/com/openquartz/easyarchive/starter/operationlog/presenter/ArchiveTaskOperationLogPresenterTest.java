package com.openquartz.easyarchive.starter.operationlog.presenter;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArchiveTaskOperationLogPresenterTest {

    @Test
    void shouldRenderCancelContentWithReasonAndResult() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(11L);
        task.setGroupId(21L);

        ArchiveTaskOperationLogPresenter presenter =
                new ArchiveTaskOperationLogPresenter(groupId -> "GROUP_" + groupId);

        String content = presenter.buildCancel(task, "人工取消", "取消中").getContent();

        assertEquals("取消归档任务：\"任务ID\" 为 \"11\"; \"分组标识\" 为 \"GROUP_21\"; \"取消原因\" 为 \"人工取消\"; \"处理结果\" 为 \"取消中\"",
                content);
    }

    @Test
    void shouldRenderCleanupResult() {
        ArchiveTaskOperationLogPresenter presenter =
                new ArchiveTaskOperationLogPresenter(groupId -> String.valueOf(groupId));

        assertEquals("清理任务日志：\"保留天数\" 为 \"30\"; \"清理结果\" 为 \"删除 8 条记录\"",
                presenter.buildCleanup(30, 8).getContent());
    }
}
