package com.openquartz.easyarchive.core.repository;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import java.util.List;

public interface ArchiveLogRepository {

    void saveTaskExecution(ArchiveGroupExecuteTask task);

    void updateTaskExecution(ArchiveGroupExecuteTask task);

    void saveTaskLog(ArchiveTaskLog log);

    ArchiveGroupExecuteTask queryTaskById(Long taskId);

    List<ArchiveGroupExecuteTask> queryTasks(int page, int size, String status);

    int countTasks(String status);

    List<ArchiveTaskLog> queryLogsByTaskId(Long taskId, int page, int size, String executePhase);

    int countLogsByTaskId(Long taskId, String executePhase);

    void updateTaskStatus(Long taskId, int status);

    int deleteByRetentionDays(int retentionDays);
}
