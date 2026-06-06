package com.openquartz.easyarchive.starter.notification.inapp;

import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.event.TaskProgressEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ArchiveInAppNotificationListenerTest {

    private final ArchiveInAppNotificationService notificationService = mock(ArchiveInAppNotificationService.class);
    private final ArchiveInAppNotificationListener listener = new ArchiveInAppNotificationListener(notificationService);

    @Test
    void shouldDispatchTaskEndEvent() {
        TaskEndEvent event = new TaskEndEvent(100L, 10L, true, 88L, 1200L, null);

        listener.onEvent(event);

        verify(notificationService).notifyTaskCompletion(event);
    }

    @Test
    void shouldIgnoreNonTaskEndEvent() {
        listener.onEvent(new TaskProgressEvent(100L, 10L, 30L, 200L, 1L, "src_order"));

        verifyNoInteractions(notificationService);
    }
}
