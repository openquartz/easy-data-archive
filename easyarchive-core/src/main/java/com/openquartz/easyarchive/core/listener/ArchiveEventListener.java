package com.openquartz.easyarchive.core.listener;

import com.openquartz.easyarchive.core.event.ArchiveEvent;

public interface ArchiveEventListener {
    void onEvent(ArchiveEvent event);
}
