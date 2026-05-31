package com.openquartz.easyarchive.core.event;

import com.openquartz.easyarchive.core.listener.ArchiveEventListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultArchiveEventPublisher implements ArchiveEventPublisher {

    private final List<ArchiveEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(ArchiveEvent event) {
        for (ArchiveEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("[ArchiveEventPublisher] listener error, eventType:{}",
                    event.getType(), e);
            }
        }
    }

    @Override
    public void registerListener(ArchiveEventListener listener) {
        listeners.add(listener);
    }
}
