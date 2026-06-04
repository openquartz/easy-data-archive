package com.openquartz.easyarchive.starter.listener;

import com.openquartz.easyarchive.core.event.ArchiveEvent;
import com.openquartz.easyarchive.core.event.RuleEndEvent;
import com.openquartz.easyarchive.core.event.RuleStartEvent;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.event.TaskProgressEvent;
import com.openquartz.easyarchive.core.event.TaskStartEvent;
import com.openquartz.easyarchive.core.listener.ArchiveEventListener;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskExecutePhaseEnum;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskLogLevelEnum;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskLogTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
public class DbArchiveLogListener implements ArchiveEventListener {

    private final ArchiveLogRepository repository;

    @Override
    public void onEvent(ArchiveEvent event) {
        if (event instanceof TaskStartEvent) {
            handleTaskStart((TaskStartEvent) event);
        } else if (event instanceof TaskEndEvent) {
            handleTaskEnd((TaskEndEvent) event);
        } else if (event instanceof RuleStartEvent) {
            handleRuleStart((RuleStartEvent) event);
        } else if (event instanceof RuleEndEvent) {
            handleRuleEnd((RuleEndEvent) event);
        } else if (event instanceof TaskProgressEvent) {
            handleTaskProgress((TaskProgressEvent) event);
        }
    }

    private void handleTaskStart(TaskStartEvent event) {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(event.getTaskId());
        task.setGroupId(event.getGroupId());
        task.setStartTime(new Date(event.getTimestamp()));
        task.setExecuteStatus(1);
        task.setProcessedRecords(0L);
        ArchiveGroupExecuteTask existing = repository.queryTaskById(event.getTaskId());
        if (existing == null) {
            repository.saveTaskExecution(task);
        } else {
            repository.updateTaskExecution(task);
        }

        saveLog(event.getTaskId(), ArchiveTaskLogTypeEnum.START, ArchiveTaskLogLevelEnum.INFO,
                "任务开始，规则数:" + event.getRuleCount(),
                ArchiveTaskExecutePhaseEnum.TASK_START, 0L, BigDecimal.ZERO, new Date(event.getTimestamp()));
    }

    private void handleTaskEnd(TaskEndEvent event) {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(event.getTaskId());
        task.setEndTime(new Date(event.getTimestamp()));
        task.setProcessedRecords(event.getTotalRows());

        String content;
        ArchiveTaskLogTypeEnum logType;
        ArchiveTaskLogLevelEnum level;

        if (event.isCancelled()) {
            task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLED);
            task.setFinishedFlag(event.getTaskId());
            content = "任务已取消" + (event.getErrorMsg() != null ? ":" + event.getErrorMsg() : "");
            logType = ArchiveTaskLogTypeEnum.CANCEL;
            level = ArchiveTaskLogLevelEnum.WARN;
        } else {
            task.setExecuteStatus(event.isSuccess() ? 2 : 3);
            task.setFinishedFlag(event.getTaskId());
            if (event.getElapsedMs() > 0) {
                task.setProcessedSpeed(BigDecimal.valueOf(event.getTotalRows() * 1000.0 / event.getElapsedMs()));
            }
            if (!event.isSuccess() && event.getErrorMsg() != null) {
                task.setErrorMsg(event.getErrorMsg());
            }
            content = event.isSuccess()
                    ? "任务完成，总行数:" + event.getTotalRows() + "，耗时:" + event.getElapsedMs() + "ms"
                    : "任务失败:" + event.getErrorMsg();
            logType = event.isSuccess() ? ArchiveTaskLogTypeEnum.FINISH : ArchiveTaskLogTypeEnum.ERROR;
            level = event.isSuccess() ? ArchiveTaskLogLevelEnum.INFO : ArchiveTaskLogLevelEnum.ERROR;
        }

        repository.updateTaskExecution(task);
        saveLog(event.getTaskId(), logType, level, content,
                ArchiveTaskExecutePhaseEnum.TASK_END, event.getTotalRows(), BigDecimal.ZERO, new Date(event.getTimestamp()));
    }

    private void handleRuleStart(RuleStartEvent event) {
        String content = "规则开始:" + event.getSourceTable() + " -> " + event.getTargetTable()
                + ", 类型:" + event.getRuleType();
        saveLog(event.getTaskId(), ArchiveTaskLogTypeEnum.START, ArchiveTaskLogLevelEnum.INFO, content,
                ArchiveTaskExecutePhaseEnum.RULE_START, 0L, BigDecimal.ZERO, new Date(event.getTimestamp()));
    }

    private void handleRuleEnd(RuleEndEvent event) {
        BigDecimal speed = event.getElapsedMs() > 0
                ? BigDecimal.valueOf(event.getProcessedRows() * 1000.0 / event.getElapsedMs())
                : BigDecimal.ZERO;
        String content = event.isSuccess()
                ? "规则完成:" + event.getSourceTable() + " -> " + event.getTargetTable()
                    + ", 处理:" + event.getProcessedRows() + "行, 耗时:" + event.getElapsedMs() + "ms"
                : "规则失败:" + event.getSourceTable() + " -> " + event.getTargetTable()
                    + ", " + event.getErrorMsg();
        ArchiveTaskLogTypeEnum logType = event.isSuccess() ? ArchiveTaskLogTypeEnum.FINISH : ArchiveTaskLogTypeEnum.ERROR;
        ArchiveTaskLogLevelEnum level = event.isSuccess() ? ArchiveTaskLogLevelEnum.INFO : ArchiveTaskLogLevelEnum.ERROR;
        saveLog(event.getTaskId(), logType, level, content,
                ArchiveTaskExecutePhaseEnum.RULE_END, event.getProcessedRows(), speed, new Date(event.getTimestamp()));
    }

    private void handleTaskProgress(TaskProgressEvent event) {
        ArchiveGroupExecuteTask task = repository.queryTaskById(event.getTaskId());
        if (task == null || task.isTerminal()) {
            return;
        }

        BigDecimal speed = BigDecimal.ZERO;
        if (event.getElapsedMs() > 0) {
            speed = BigDecimal.valueOf(event.getProcessedRecords() * 1000.0 / event.getElapsedMs())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        task.setProcessedRecords(event.getProcessedRecords());
        task.setProcessedSpeed(speed);
        task.setHeartbeatTime(new Date());
        repository.updateTaskExecution(task);

        String content = String.format("进度: 已处理 %d 行, 速度 %s 行/秒, 当前表: %s",
                event.getProcessedRecords(), speed, event.getSourceTable());
        saveLog(event.getTaskId(), ArchiveTaskLogTypeEnum.PROGRESS, ArchiveTaskLogLevelEnum.INFO, content,
                ArchiveTaskExecutePhaseEnum.TASK_PROGRESS, event.getProcessedRecords(), speed, new Date(event.getTimestamp()));
    }

    private void saveLog(Long taskId, ArchiveTaskLogTypeEnum logType, ArchiveTaskLogLevelEnum logLevel,
                         String logContent, ArchiveTaskExecutePhaseEnum executePhase,
                         Long processedCount, BigDecimal processSpeed, Date logTime) {
        ArchiveTaskLog log = new ArchiveTaskLog();
        log.setTaskId(taskId);
        log.setLogType(logType);
        log.setLogLevel(logLevel);
        log.setLogContent(logContent);
        log.setExecutePhase(executePhase);
        log.setProcessedCount(processedCount);
        log.setProcessSpeed(processSpeed);
        log.setLogTime(logTime);
        repository.saveTaskLog(log);
    }
}
