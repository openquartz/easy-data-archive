package com.openquartz.easyarchive.core;

import com.openquartz.easyarchive.core.executor.ArchiveExecutor;
import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.core.rule.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.TableRule;
import com.openquartz.easyarchive.core.rule.TableRuleLoader;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import com.openquartz.easyarchive.common.concurrent.ILock;
import com.openquartz.easyarchive.common.util.ExceptionUtils;

/**
 * ArchiveService
 *
 * @author svnee
 */
@Slf4j
public class ArchiveGroupExecutor implements Runnable {

    private final String sourceUrl;
    private final String targetUrl;

    private final ILock lock;
    private final TableRuleLoader loader;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final ArchiveConfig archiveConfig;

    public ArchiveGroupExecutor(ArchiveConfig archiveConfig, ILock lock, TableRuleLoader loader) {
        this.sourceUrl = archiveConfig.getSourceConnection();
        this.targetUrl = archiveConfig.getTargetConnection();
        this.archiveConfig = archiveConfig;
        this.loader = loader;
        this.lock = lock;
    }

    /**
     * 开始执行
     */
    @Override
    public void run() {

        List<ArchiveGroupItem> configs = loader.load();

        Map<Long, List<ArchiveGroupItem>> configGroup = configs.stream()
            .collect(Collectors.groupingBy(ArchiveGroupItem::getGroupId));

        CountDownLatch latch = new CountDownLatch(configGroup.size());
        for (Entry<Long, List<ArchiveGroupItem>> entry : configGroup.entrySet()) {
            threadPool.submit(() -> {
                try {
                    boolean success = lock.lock(entry.getKey());
                    if (success) {
                        List<ArchiveGroupItem> singleGroupConfig = entry.getValue();
                        singleGroupConfig.sort((Comparator.comparing(ArchiveGroupItem::getPriority)));
                        try {
                            execute(singleGroupConfig);
                        } catch (Exception ex) {
                            log.error("ArchiveExecutor-archive-error!,Group:{},Ex:", entry.getKey(), ex);
                        } finally {
                            lock.unlock(entry.getKey());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await();

            // shutdown thread pool
            threadPool.shutdown();
            threadPool.awaitTermination(100, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            ExceptionUtils.rethrow(exception);
        }
    }

    private void execute(List<ArchiveGroupItem> rules) {
        new ArchiveExecutor(sourceUrl, targetUrl, archiveConfig, rules).run();
    }
}
