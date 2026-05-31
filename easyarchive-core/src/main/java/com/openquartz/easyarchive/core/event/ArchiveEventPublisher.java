package com.openquartz.easyarchive.core.event;

import com.openquartz.easyarchive.core.listener.ArchiveEventListener;

public interface ArchiveEventPublisher {
    void publish(ArchiveEvent event);
    void registerListener(ArchiveEventListener listener);
}
