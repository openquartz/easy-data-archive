package com.openquartz.easyarchive.core.event;

import com.openquartz.easyarchive.core.listener.ArchiveEventListener;

public class NoOpArchiveEventPublisher implements ArchiveEventPublisher {

    public static final NoOpArchiveEventPublisher INSTANCE = new NoOpArchiveEventPublisher();

    @Override
    public void publish(ArchiveEvent event) { }

    @Override
    public void registerListener(ArchiveEventListener listener) { }
}
