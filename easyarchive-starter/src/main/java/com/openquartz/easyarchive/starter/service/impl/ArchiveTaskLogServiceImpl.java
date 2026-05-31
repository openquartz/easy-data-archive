package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ArchiveTaskLogServiceImpl implements ArchiveTaskLogService {

    private final ArchiveLogRepository archiveLogRepository;

    @Override
    public Map<String, Object> queryTasks(int page, int size, String status) {
        List<ArchiveGroupExecuteTask> list = archiveLogRepository.queryTasks(page, size, status);
        int total = archiveLogRepository.countTasks(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Object queryTaskById(Long taskId) {
        return archiveLogRepository.queryTaskById(taskId);
    }

    @Override
    public Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        List<ArchiveTaskLog> list = archiveLogRepository.queryLogsByTaskId(taskId, page, size, executePhase);
        int total = archiveLogRepository.countLogsByTaskId(taskId, executePhase);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanup(int retentionDays) {
        return archiveLogRepository.deleteByRetentionDays(retentionDays);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, String cancelReason) {
        ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        if (task.isTerminal()) {
            throw new IllegalStateException("任务已结束，无法取消");
        }
        if (task.getExecuteStatus() != null
                && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_CANCELLING) {
            return; // already cancelling, idempotent
        }
        if (task.getExecuteStatus() != null
                && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_WAITING) {
            archiveLogRepository.updateTaskStatus(taskId, ArchiveGroupExecuteTask.STATUS_CANCELLED);
        } else {
            archiveLogRepository.updateTaskStatus(taskId, ArchiveGroupExecuteTask.STATUS_CANCELLING);
        }

        ArchiveTaskLog log = new ArchiveTaskLog();
        log.setTaskId(taskId);
        log.setLogType("CANCEL");
        log.setLogLevel("WARN");
        log.setLogContent("任务取消请求" + (cancelReason != null ? ":" + cancelReason : ""));
        log.setExecutePhase("TASK_END");
        log.setProcessedCount(task.getProcessedRecords() != null ? task.getProcessedRecords() : 0L);
        log.setProcessSpeed(BigDecimal.ZERO);
        log.setLogTime(new Date());
        archiveLogRepository.saveTaskLog(log);
    }
}
