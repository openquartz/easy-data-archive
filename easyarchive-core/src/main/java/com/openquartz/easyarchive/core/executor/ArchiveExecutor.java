package com.openquartz.easyarchive.core.executor;

import com.google.common.base.Stopwatch;
import com.openquartz.easyarchive.common.api.model.TableInfo;
import com.openquartz.easyarchive.common.util.DateUtils;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.expr.ExpressionService;
import com.openquartz.easyarchive.core.property.ArchiveConfig;

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
 * ArchiveExecutor
 *
 * @author svnee
 */
@Slf4j
public class ArchiveExecutor implements Runnable {

    private final ArchiveConnection sourceConnection;
    private final ArchiveConnection sinkConnection;
    private final ArchiveConfig archiveConfig;
    private final List<ArchiveGroupItem> ruleList;
    private final Long taskId;

    public ArchiveExecutor(ArchiveConnection sourceConnection,
                           ArchiveConnection sinkConnection,
                           ArchiveConfig archiveConfig,
                           List<ArchiveGroupItem> ruleList,
                           Long taskId) {
        this.sourceConnection = sourceConnection;
        this.sinkConnection = sinkConnection;
        this.archiveConfig = archiveConfig;
        this.ruleList = ruleList;
        this.taskId = taskId;
    }

    /**
     * run execute
     */
    @Override
    public void run() {

        long totalProcessRecords = 0;
        long startTime = System.currentTimeMillis();

        for (ArchiveGroupItem rule : ruleList) {

            // 检查任务是否被取消
            checkCancellation();

            // 开始打印日志

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

                    // 按照时间滚动
                    if (rule instanceof ArchiveGroupItemByTime) {

                        ArchiveGroupItemByTime byTimeRule = (ArchiveGroupItemByTime) rule;
                        Date endDate = DateUtils.floorDay(DateUtils.addDays(new Date(), -byTimeRule.getKeepDay()));
                        Date startDate = DateUtils.floorDay(byTimeRule.getStartTime());

                        // 记录执行日志

                        for (Date curDate = startDate; Objects.requireNonNull(curDate).compareTo(endDate) < 0; ) {

                            // 检查任务是否被执行取消
                            checkCancellation();

                            Date curEndDate = DateUtils.addHours(curDate, 1);
                            int batchRows = executor.execute(curDate, curEndDate, byTimeRule.getStepCount());
                            executeRows += batchRows;
                            totalProcessRecords += batchRows;

                            // 更新进度
                            updateProcess(totalProcessRecords,startTime);

                            curDate = curEndDate;
                        }
                    }

                    // 按照id 进行滚动
                    if (rule instanceof ArchiveGroupItemById){

                        ArchiveGroupItemById byIdRule = (ArchiveGroupItemById) rule;

                        String startIdStr = ExpressionService.getInstance().parse(byIdRule.getStartId());
                        Long startId = Long.valueOf(startIdStr);
                        String endIdStr = ExpressionService.getInstance().parse(byIdRule.getEndId());
                        Long endId = Long.valueOf(endIdStr);

                        // 记录执行日志

                        while(Objects.requireNonNull(startId).compareTo(endId)<0){

                            // 校验是否被取消
                            checkCancellation();

                            Long curEndId = startId + byIdRule.getStepRounds();
                            int batchRows = executor.execute(startId, curEndId, byIdRule.getStepCount());

                            executeRows += batchRows;
                            totalProcessRecords += batchRows;

                            // 更新进度
                            updateProcess(totalProcessRecords, startTime);

                            startId = curEndId;
                        }
                    }

                    watch.stop();


                    // 执行时间 统计总耗时
                    long elapsedMilliseconds = watch.elapsed(TimeUnit.MILLISECONDS);
                    log.info("[ArchiveExecutor]{}->{},execute error!,archive-rows:{},take {}ms",
                        rule.getSourceTable(),
                        rule.getTargetTable(),
                        executeRows,
                        elapsedMilliseconds);

                    // 记录进度

                    // 记录执行日志

                } catch (Exception e) {

                    watch.stop();
                    long elapsed = watch.elapsed(TimeUnit.MILLISECONDS);
                    // 拼接执行日志

                    log.error("[ArchiveExecutor]{}->{},execute error", rule.getSourceTable(), rule.getTargetTable(), e);

                    ExceptionUtils.rethrow(e);
                }
            } catch (Exception ex) {
                log.error("[ArchiveExecutor]{},execute error!", rule.getSourceTable(), ex);
                throw ex;
            }
        }
    }

    private void updateProcess(long totalProcessRecords, long startTime) {
        // 更新进度 ToDo
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

    private void checkCancellation() {

    }
}
