package com.openquartz.easyarchive.starter.repository;

import com.openquartz.easyarchive.common.enums.ArchiveTaskStatusEnum;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveTaskLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class JdbcArchiveLogRepository implements ArchiveLogRepository {

    private final ArchiveGroupExecuteTaskMapper executeTaskMapper;
    private final ArchiveTaskLogMapper taskLogMapper;

    @Override
    public void saveTaskExecution(ArchiveGroupExecuteTask task) {
        executeTaskMapper.insert(task);
    }

    @Override
    public void updateTaskExecution(ArchiveGroupExecuteTask task) {
        normalizeFinishedFlag(task);
        executeTaskMapper.update(task);
    }

    @Override
    public void saveTaskLog(ArchiveTaskLog log) {
        taskLogMapper.insert(log);
    }

    @Override
    public ArchiveGroupExecuteTask queryTaskById(Long taskId) {
        return executeTaskMapper.selectById(taskId);
    }

    @Override
    public List<ArchiveGroupExecuteTask> queryTasks(int page, int size, String status) {
        int offset = (page - 1) * size;
        return executeTaskMapper.selectPage(offset, size, status);
    }

    @Override
    public int countTasks(String status) {
        return executeTaskMapper.count(status);
    }

    @Override
    public List<ArchiveTaskLog> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        int offset = (page - 1) * size;
        return taskLogMapper.selectByTaskId(taskId, offset, size, executePhase);
    }

    @Override
    public int countLogsByTaskId(Long taskId, String executePhase) {
        return taskLogMapper.countByTaskId(taskId, executePhase);
    }

    @Override
    public void updateTaskStatus(Long taskId, int status) {
        if (ArchiveTaskStatusEnum.isTerminal(status)) {
            ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
            task.setId(taskId);
            task.setExecuteStatus(status);
            task.setFinishedFlag(taskId);
            executeTaskMapper.update(task);
            return;
        }
        executeTaskMapper.updateExecuteStatus(taskId, status);
    }

    @Override
    public int deleteByRetentionDays(int retentionDays) {
        int taskLogs = taskLogMapper.deleteByRetentionDays(retentionDays);
        int tasks = executeTaskMapper.deleteByRetentionDays(retentionDays);
        return taskLogs + tasks;
    }

    private void normalizeFinishedFlag(ArchiveGroupExecuteTask task) {
        if (task == null || task.getId() == null) {
            return;
        }
        if (ArchiveTaskStatusEnum.isTerminal(task.getExecuteStatus()) && task.getFinishedFlag() == null) {
            task.setFinishedFlag(task.getId());
        }
    }
}
