package com.openquartz.easyarchive.starter.notification.inapp;

import com.openquartz.easyarchive.core.event.ArchiveEvent;
import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.listener.ArchiveEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveInAppNotificationListener implements ArchiveEventListener {

    private final ArchiveInAppNotificationService notificationService;

    @Override
    public void onEvent(ArchiveEvent event) {
        if (!(event instanceof TaskEndEvent)) {
            return;
        }
        try {
            notificationService.notifyTaskCompletion((TaskEndEvent) event);
        } catch (Exception ex) {
            log.error("[ArchiveInAppNotificationListener] failed to persist in-app notification, taskId:{}",
                    event.getTaskId(), ex);
        }
    }
}
