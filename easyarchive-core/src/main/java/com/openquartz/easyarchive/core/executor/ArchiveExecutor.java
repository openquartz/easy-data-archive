package com.openquartz.easyarchive.core.executor;

import com.openquartz.easyarchive.core.property.ArchiveConfig;

import java.util.List;

import com.openquartz.easyarchive.core.rule.ArchiveGroupItem;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.api.Sink;
import com.openquartz.easyarchive.common.api.PageSource;
import com.openquartz.easyarchive.core.SyncExecutor;
import com.openquartz.easyarchive.sink.mysql.MysqlSink;
import com.openquartz.easyarchive.source.mysql.MysqlSource;

/**
 * ArchiveExecutor
 *
 * @author svnee
 */
@Slf4j
public class ArchiveExecutor implements Runnable {

    private final String sourceUrl;
    private final String targetUrl;
    private final ArchiveConfig archiveConfig;
    private final List<ArchiveGroupItem> ruleList;

    public ArchiveExecutor(String sourceUrl, String targetUrl, ArchiveConfig archiveConfig,
        List<ArchiveGroupItem> ruleList) {
        this.sourceUrl = sourceUrl;
        this.targetUrl = targetUrl;
        this.archiveConfig = archiveConfig;
        this.ruleList = ruleList;
    }

    /**
     * run execute
     */
    @Override
    public void run() {
        for (ArchiveGroupItem rule : ruleList) {
            try {
                try (PageSource reader = new MysqlSource(sourceUrl, rule.getGroupId(), rule.getSourceTable(),
                    rule.getFetchSql(), rule.enableClean(), rule.getDeleteWhere());

                    Sink sink = new MysqlSink(targetUrl, rule.getTargetTable());
                    SyncExecutor executor = new SyncExecutor(archiveConfig, reader, sink)) {

//                    Date endDate = DateUtils.floorDay(DateUtils.addDays(new Date(), -rule.getKeepDay()));
//                    Date startDate = DateUtils.floorDay(rule.getStartTime());
//                    Stopwatch watch = Stopwatch.createStarted();
//
//                    for (Date curDate = startDate; Objects.requireNonNull(curDate).compareTo(endDate) < 0; ) {
//                        Date curEndDate = DateUtils.addHours(curDate, 1);
//                        executor.execute(curDate, curEndDate, rule.getStepCount());
//                        curDate = curEndDate;
//                    }
//
//                    watch.stop();

                    // 执行时间 统计总耗时
//                    long elapsedMilliseconds = watch.elapsed(TimeUnit.MILLISECONDS);
//                    log.info("[ArchiveExecutor]{}->{},execute error!, startDate:{},endDate:{},take {}ms",
//                        rule.getSourceTable(),
//                        rule.getTargetTable(),
//                        startDate, endDate,
//                        elapsedMilliseconds);
                } catch (Exception e) {
                    log.error("[ArchiveExecutor]{}->{},execute error", rule.getSourceTable(), rule.getTargetTable(), e);
                }
            } catch (Exception ex) {
                log.error("[ArchiveExecutor]{},execute error!", rule.getSourceTable(), ex);
                throw ex;
            }
        }
    }
}
