package com.openquartz.easyarchive.starter.task;

import com.openquartz.easyarchive.starter.mapper.InAppNotificationMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationRecipientMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InAppNotificationCleanupTask {

    private static final int RETENTION_DAYS = 7;

    private final InAppNotificationRecipientMapper recipientMapper;
    private final InAppNotificationMapper notificationMapper;

    @Scheduled(cron = "0 10 3 * * ?")
    public void cleanup() {
        int deletedRecipients = recipientMapper.deleteReadModelOlderThan(RETENTION_DAYS);
        int deletedNotifications = notificationMapper.deleteOrphansOlderThan(RETENTION_DAYS);
        log.info("[InAppNotificationCleanup] cleaned {} recipient rows and {} notifications older than {} days",
                deletedRecipients, deletedNotifications, RETENTION_DAYS);
    }
}
