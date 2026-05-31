package com.openquartz.easyarchive.core;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.event.TaskStartEvent;
import com.openquartz.easyarchive.core.executor.ArchiveExecutor;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.util.ExceptionUtils;
import com.openquartz.easyarchive.core.exception.TaskCancelledException;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;

@Slf4j
public class ArchiveGroupExecutor implements Runnable {

    private final ArchiveRuleLoader loader;
    private final ArchiveConfig archiveConfig;
    private final ArchiveGroupExecuteTask executeTask;
    private final Pair<ArchiveConnection, ArchiveConnection> connectionInfo;
    private final ArchiveEventPublisher publisher;
    private final ArchiveLogRepository archiveLogRepository;

    public ArchiveGroupExecutor(ArchiveRuleLoader loader,
                                ArchiveConfig archiveConfig,
                                ArchiveGroupExecuteTask executeTask,
                                Pair<ArchiveConnection, ArchiveConnection> connectionInfo,
                                ArchiveEventPublisher publisher,
                                ArchiveLogRepository archiveLogRepository) {
        this.loader = loader;
        this.archiveConfig = archiveConfig;
        this.executeTask = executeTask;
        this.connectionInfo = connectionInfo;
        this.publisher = publisher;
        this.archiveLogRepository = archiveLogRepository;
    }

    @Override
    public void run() {
        long startMs = System.currentTimeMillis();

        try {
            List<ArchiveGroupItem> configs = loader.load();
            configs.sort(Comparator.comparing(ArchiveGroupItem::getPriority));

            log.info("[ArchiveGroupExecutor#run] start archive, taskId:{}, ruleCount:{}",
                executeTask.getId(), configs.size());

            publisher.publish(new TaskStartEvent(
                executeTask.getId(), executeTask.getGroupId(), configs.size()));

            long totalRows = doExecute(configs);

            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                true, totalRows, elapsed, null));

            log.info("[ArchiveGroupExecutor#run] archive completed, taskId:{}, elapsed:{}ms",
                executeTask.getId(), elapsed);

        } catch (TaskCancelledException ex) {
            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                false, 0L, elapsed, "Task cancelled", true));

            log.info("[ArchiveGroupExecutor#run] archive cancelled, taskId:{}", executeTask.getId());

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                false, 0L, elapsed, ex.getMessage()));

            log.error("[ArchiveGroupExecutor#run] archive failed, taskId:{}", executeTask.getId(), ex);
            ExceptionUtils.rethrow(ex);
        }
    }

    private long doExecute(List<ArchiveGroupItem> configs) {
        ArchiveExecutor executor = new ArchiveExecutor(connectionInfo.getKey(), connectionInfo.getValue(),
            archiveConfig, configs, executeTask.getId(), executeTask.getGroupId(), publisher, archiveLogRepository);
        executor.run();
        return executor.getTotalProcessRecords();
    }
}
