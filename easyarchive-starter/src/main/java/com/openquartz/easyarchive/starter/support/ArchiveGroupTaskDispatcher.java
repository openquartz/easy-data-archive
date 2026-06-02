package com.openquartz.easyarchive.starter.support;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.core.ArchiveGroupExecutor;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.event.ArchiveEventPublisher;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class ArchiveGroupTaskDispatcher {

    private final ArchiveConfig archiveConfig;
    private final ArchiveEventPublisher publisher;
    private final ArchiveLogRepository archiveLogRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    public ArchiveGroupTaskDispatcher(ArchiveConfig archiveConfig,
                                      ArchiveEventPublisher publisher,
                                      ArchiveLogRepository archiveLogRepository) {
        this.archiveConfig = archiveConfig;
        this.publisher = publisher;
        this.archiveLogRepository = archiveLogRepository;
    }

    public void dispatch(ArchiveRuleLoader loader,
                         ArchiveGroupExecuteTask task,
                         Pair<ArchiveConnection, ArchiveConnection> connections) {
        ArchiveGroupExecutor executor = new ArchiveGroupExecutor(
                loader, archiveConfig, task, connections, publisher, archiveLogRepository);
        executorService.submit(executor);
    }

    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
    }
}
