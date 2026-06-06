package com.openquartz.easyarchive.starter.task;

import com.openquartz.easyarchive.starter.mapper.InAppNotificationMapper;
import com.openquartz.easyarchive.starter.mapper.InAppNotificationRecipientMapper;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InAppNotificationCleanupTaskTest {

    private final InAppNotificationRecipientMapper recipientMapper = mock(InAppNotificationRecipientMapper.class);
    private final InAppNotificationMapper notificationMapper = mock(InAppNotificationMapper.class);
    private final InAppNotificationCleanupTask cleanupTask =
            new InAppNotificationCleanupTask(recipientMapper, notificationMapper);

    @Test
    void shouldCleanupNotificationsOlderThanSevenDays() {
        cleanupTask.cleanup();

        verify(recipientMapper).deleteReadModelOlderThan(7);
        verify(notificationMapper).deleteOrphansOlderThan(7);
    }
}
