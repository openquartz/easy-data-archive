package com.openquartz.easyarchive.starter.notification;

import com.openquartz.easyarchive.core.event.ArchiveEvent;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.listener.ArchiveEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class ArchiveNotificationListener implements ArchiveEventListener {

    private final ArchiveNotificationService notificationService;

    @Override
    public void onEvent(ArchiveEvent event) {
        if (!(event instanceof TaskEndEvent)) {
            return;
        }
        try {
            notificationService.notifyTaskCompletion((TaskEndEvent) event);
        } catch (Exception ex) {
            log.error("[ArchiveNotificationListener] failed to send notification, taskId:{}",
                    event.getTaskId(), ex);
        }
    }
}
