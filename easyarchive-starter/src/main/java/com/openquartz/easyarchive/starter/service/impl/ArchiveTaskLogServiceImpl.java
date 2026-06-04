package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskExecutePhaseEnum;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskLogLevelEnum;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskLogTypeEnum;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveTaskOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
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
    private final ArchiveGroupExecuteTaskMapper archiveGroupExecuteTaskMapper;
    private final DataPermissionService dataPermissionService;
    private final ArchiveTaskOperationLogPresenter archiveTaskOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public Map<String, Object> queryTasks(int page, int size, String status) {
        List<ArchiveGroupExecuteTask> list;
        int total;
        if (dataPermissionService.isAdmin()) {
            list = archiveLogRepository.queryTasks(page, size, status);
            total = archiveLogRepository.countTasks(status);
        } else {
            int offset = (page - 1) * size;
            Long userId = dataPermissionService.getCurrentUser().getUserId();
            list = archiveGroupExecuteTaskMapper.selectPageByUser(userId, offset, size, status);
            total = archiveGroupExecuteTaskMapper.countByUser(userId, status);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Object queryTaskById(Long taskId) {
        dataPermissionService.assertTaskReadable(taskId);
        return archiveLogRepository.queryTaskById(taskId);
    }

    @Override
    public Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        dataPermissionService.assertTaskReadable(taskId);
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
        int deleted = archiveLogRepository.deleteByRetentionDays(retentionDays);
        operationLogRecorder.record(archiveTaskOperationLogPresenter.buildCleanup(retentionDays, deleted));
        return deleted;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, String cancelReason) {
        dataPermissionService.assertTaskReadable(taskId);
        ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(taskId);
        if (task == null) {
            String message = "任务不存在: " + taskId;
            operationLogRecorder.recordFailure(message);
            throw new StarterManageException(StarterErrorCode.TASK_NOT_FOUND, message);
        }
        if (task.isTerminal()) {
            operationLogRecorder.recordFailure("任务已结束，无法取消");
            throw new StarterManageException(StarterErrorCode.ARCHIVE_TASK_TERMINAL_CANNOT_CANCEL);
        }
        if (task.getExecuteStatus() != null
                && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_CANCELLING) {
            operationLogRecorder.record(archiveTaskOperationLogPresenter.buildCancel(
                    task, cancelReason, "重复请求，无需处理"));
            return; // already cancelling, idempotent
        }
        String result;
        if (task.getExecuteStatus() != null
                && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_WAITING) {
            archiveLogRepository.updateTaskStatus(taskId, ArchiveGroupExecuteTask.STATUS_CANCELLED);
            result = "已取消";
        } else {
            archiveLogRepository.updateTaskStatus(taskId, ArchiveGroupExecuteTask.STATUS_CANCELLING);
            result = "取消中";
        }
        operationLogRecorder.record(archiveTaskOperationLogPresenter.buildCancel(task, cancelReason, result));

        ArchiveTaskLog log = new ArchiveTaskLog();
        log.setTaskId(taskId);
        log.setLogType(ArchiveTaskLogTypeEnum.CANCEL);
        log.setLogLevel(ArchiveTaskLogLevelEnum.WARN);
        log.setLogContent("任务取消请求" + (cancelReason != null ? ":" + cancelReason : ""));
        log.setExecutePhase(ArchiveTaskExecutePhaseEnum.TASK_END);
        log.setProcessedCount(task.getProcessedRecords() != null ? task.getProcessedRecords() : 0L);
        log.setProcessSpeed(BigDecimal.ZERO);
        log.setLogTime(new Date());
        archiveLogRepository.saveTaskLog(log);
    }
}
