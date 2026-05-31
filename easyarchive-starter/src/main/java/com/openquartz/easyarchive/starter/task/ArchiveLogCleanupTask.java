package com.openquartz.easyarchive.starter.task;

import com.openquartz.easyarchive.core.property.ArchiveConfig;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "sync.log.enabled", havingValue = "true", matchIfMissing = true)
public class ArchiveLogCleanupTask {

    private final ArchiveTaskLogService taskLogService;
    private final ArchiveConfig archiveConfig;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int retentionDays = archiveConfig.getLogRetentionDays();
        int deleted = taskLogService.cleanup(retentionDays);
        log.info("[ArchiveLogCleanup] cleaned {} log records older than {} days",
                deleted, retentionDays);
    }
}
