package com.openquartz.easyarchive.starter.notification;

import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.event.TaskProgressEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ArchiveNotificationListenerTest {

    private final ArchiveNotificationService notificationService = mock(ArchiveNotificationService.class);
    private final ArchiveNotificationListener listener = new ArchiveNotificationListener(notificationService);

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
