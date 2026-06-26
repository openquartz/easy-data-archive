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
import com.openquartz.easyarchive.starter.model.dto.TaskLogVO;
import com.openquartz.easyarchive.starter.model.dto.TaskVO;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveTaskOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.utils.TaskConvertUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ArchiveTaskLogServiceImpl implements ArchiveTaskLogService {

    private final ArchiveLogRepository archiveLogRepository;
    private final ArchiveGroupExecuteTaskMapper archiveGroupExecuteTaskMapper;
    private final ArchiveResourceAccessService archiveResourceAccessService;
    private final CurrentUserService currentUserService;
    private final ArchiveTaskOperationLogPresenter archiveTaskOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;
    private final ArchiveTaskGroupNameResolver archiveTaskGroupNameResolver;

    @Override
    public Map<String, Object> queryTasks(int page, int size, String status, Long groupId) {
        List<ArchiveGroupExecuteTask> list;
        int total;
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            list = archiveLogRepository.queryTasks(page, size, status, groupId);
            total = archiveLogRepository.countTasks(status, groupId);
        } else {
            int offset = (page - 1) * size;
            Long userId = currentUser.getUserId();
            list = archiveGroupExecuteTaskMapper.selectPageByUser(userId, offset, size, status, groupId);
            total = archiveGroupExecuteTaskMapper.countByUser(userId, status, groupId);
        }
        archiveTaskGroupNameResolver.fillGroupNames(list);
        Map<String, Object> result = new HashMap<>();
        result.put("list", TaskConvertUtils.fromEntityTaskList(list));
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public TaskVO queryTaskById(Long taskId) {
        archiveResourceAccessService.assertTaskAccessible(taskId);
        ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(taskId);
        archiveTaskGroupNameResolver.fillGroupNames(Collections.singletonList(task));
        return TaskConvertUtils.fromEntity(task);
    }

    @Override
    public Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        archiveResourceAccessService.assertTaskAccessible(taskId);
        List<ArchiveTaskLog> list = archiveLogRepository.queryLogsByTaskId(taskId, page, size, executePhase);
        int total = archiveLogRepository.countLogsByTaskId(taskId, executePhase);
        Map<String, Object> result = new HashMap<>();
        result.put("list", TaskConvertUtils.fromEntityLogList(list));
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
        archiveResourceAccessService.assertTaskAccessible(taskId);
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
