package com.openquartz.easyarchive.core.executor;

import com.google.common.base.Stopwatch;
import com.openquartz.easyarchive.common.api.model.TableInfo;
import com.openquartz.easyarchive.common.util.DateUtils;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.RuleEndEvent;
import com.openquartz.easyarchive.core.event.RuleStartEvent;
import com.openquartz.easyarchive.core.event.TaskProgressEvent;
import com.openquartz.easyarchive.core.expr.ExpressionService;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.exception.TaskCancelledException;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.core.sink.EmptySink;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.PageSource;
import com.openquartz.easyarchive.core.SyncExecutor;
import com.openquartz.easyarchive.core.sink.mysql.MysqlSink;
import com.openquartz.easyarchive.core.source.mysql.MysqlSource;

/**
 * 归档执行任务
 */
@Slf4j
public class ArchiveExecutor implements Runnable {

    private final ArchiveConnection sourceConnection;
    private final ArchiveConnection sinkConnection;
    private final ArchiveConfig archiveConfig;
    private final List<ArchiveGroupItem> ruleList;
    private final Long taskId;
    private final ArchiveEventPublisher publisher;

    private final Long groupId;
    private final ArchiveLogRepository archiveLogRepository;

    private Long currentRuleId;
    private String currentSourceTable;
    private long totalProcessRecords;
    private long lastProgressUpdateTime = 0;

    public ArchiveExecutor(ArchiveConnection sourceConnection,
                           ArchiveConnection sinkConnection,
                           ArchiveConfig archiveConfig,
                           List<ArchiveGroupItem> ruleList,
                           Long taskId,
                           Long groupId,
                           ArchiveEventPublisher publisher,
                           ArchiveLogRepository archiveLogRepository) {
        this.sourceConnection = sourceConnection;
        this.sinkConnection = sinkConnection;
        this.archiveConfig = archiveConfig;
        this.ruleList = ruleList;
        this.taskId = taskId;
        this.groupId = groupId;
        this.publisher = publisher;
        this.archiveLogRepository = archiveLogRepository;
    }

    @Override
    public void run() {

        long startTime = System.currentTimeMillis();

        for (ArchiveGroupItem rule : ruleList) {

            // 校验是否被取消和中断
            checkCancellation();

            String ruleType = (rule instanceof ArchiveGroupItemByTime) ? "TIME" : "ID";

            this.currentRuleId = rule.getId();
            this.currentSourceTable = ExpressionService.getInstance().parse(rule.getSourceTable());

            publisher.publish(new RuleStartEvent(
                taskId, rule.getGroupId(),
                rule.getSourceTable(), rule.getTargetTable(), ruleType));

            Stopwatch watch = Stopwatch.createStarted();
            int executeRows = 0;
            String fetchSql = ExpressionService.getInstance().parse(rule.getFetchSql());
            int effectivePauseMs = resolvePauseMs(rule);

            try {
                try (PageSource reader = new MysqlSource(sourceConnection,
                        rule.getGroupId(),
                        TableInfo.of(ExpressionService.getInstance().parse(rule.getSourceTable()), rule.getIdColumn()),
                        fetchSql,
                        rule.isEnableClean(),
                        rule.getDeleteWhere());
                     Sink sink = createSink(rule, sinkConnection);
                     SyncExecutor executor = new SyncExecutor(archiveConfig, reader, sink, effectivePauseMs)) {

                    if (rule instanceof ArchiveGroupItemByTime) {
                        ArchiveGroupItemByTime byTimeRule = (ArchiveGroupItemByTime) rule;
                        Date endDate = DateUtils.floorDay(DateUtils.addDays(new Date(), -byTimeRule.getKeepDay()));
                        Date startDate = DateUtils.floorDay(byTimeRule.getStartTime());

                        for (Date curDate = startDate; Objects.requireNonNull(curDate).compareTo(endDate) < 0; ) {
                            // 校验是否被取消和中断
                            checkCancellation();
                            Date curEndDate = DateUtils.addHours(curDate, 1);
                            int batchRows = executor.execute(curDate, curEndDate, byTimeRule.getStepCount());
                            executeRows += batchRows;
                            totalProcessRecords += batchRows;
                            // 上报进度
                            updateProcess(totalProcessRecords, startTime);
                            curDate = curEndDate;
                        }
                    }

                    if (rule instanceof ArchiveGroupItemById) {
                        ArchiveGroupItemById byIdRule = (ArchiveGroupItemById) rule;
                        String startIdStr = ExpressionService.getInstance().parse(byIdRule.getStartId());
                        Long startId = Long.valueOf(startIdStr);
                        String endIdStr = ExpressionService.getInstance().parse(byIdRule.getEndId());
                        Long endId = Long.valueOf(endIdStr);

                        while (Objects.requireNonNull(startId).compareTo(endId) < 0) {
                            // 校验是否被取消和中断
                            checkCancellation();
                            Long curEndId = startId + byIdRule.getStepRounds();
                            int batchRows = executor.execute(startId, curEndId, byIdRule.getStepCount());
                            executeRows += batchRows;
                            totalProcessRecords += batchRows;
                            // 上报进度
                            updateProcess(totalProcessRecords, startTime);
                            startId = curEndId;
                        }
                    }

                    watch.stop();
                    long elapsedMilliseconds = watch.elapsed(TimeUnit.MILLISECONDS);

                    log.info("[ArchiveExecutor] {} -> {}, execute completed, archive-rows:{}, take {}ms",
                        rule.getSourceTable(), rule.getTargetTable(), executeRows, elapsedMilliseconds);

                    publisher.publish(new RuleEndEvent(
                        taskId, rule.getGroupId(),
                        rule.getSourceTable(), rule.getTargetTable(),
                        ruleType, true, executeRows, elapsedMilliseconds, null));

                } catch (Exception e) {
                    watch.stop();
                    long elapsed = watch.elapsed(TimeUnit.MILLISECONDS);

                    publisher.publish(new RuleEndEvent(
                        taskId, rule.getGroupId(),
                        rule.getSourceTable(), rule.getTargetTable(),
                        ruleType, false, executeRows, elapsed, e.getMessage()));

                    log.error("[ArchiveExecutor] {} -> {}, execute error",
                        rule.getSourceTable(), rule.getTargetTable(), e);
                    ExceptionUtils.rethrow(e);
                }
            } catch (Exception ex) {
                log.error("[ArchiveExecutor] {}, execute error!", rule.getSourceTable(), ex);
                throw ex;
            }
        }
    }

    private void updateProcess(long totalProcessRecords, long startTime) {
        long now = System.currentTimeMillis();
        if (now - lastProgressUpdateTime < archiveConfig.getProgressUpdateIntervalMs()) {
            return;
        }
        lastProgressUpdateTime = now;
        long elapsedMs = now - startTime;
        publisher.publish(new TaskProgressEvent(
            taskId, groupId, totalProcessRecords, elapsedMs,
            currentRuleId, currentSourceTable
        ));
    }

    private Sink createSink(ArchiveGroupItem rule, ArchiveConnection targetConnection) {
        if (!rule.isEnableWrite()) {
            return new EmptySink();
        }
        return new MysqlSink(targetConnection, rule.getTargetTable());
    }

    private int resolvePauseMs(ArchiveGroupItem rule) {
        if (rule.getPauseMs() != null && rule.getPauseMs() != 0) {
            return rule.getPauseMs();
        }
        return archiveConfig.getArchivePauseMs();
    }

    public long getTotalProcessRecords() {
        return totalProcessRecords;
    }

    private void checkCancellation() {
        try {
            ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(taskId);
            if (task != null && task.getExecuteStatus() != null
                    && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_CANCELLING) {
                throw new TaskCancelledException(taskId);
            }
        } catch (TaskCancelledException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[ArchiveExecutor] Failed to check cancellation status for task {}: {}",
                    taskId, e.getMessage());
        }
    }
}
