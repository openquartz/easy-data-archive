package com.openquartz.easyarchive.core.event;

import java.util.UUID;

public abstract class ArchiveEvent {

    private final String eventId;
    private final ArchiveEventType type;
    private final Long taskId;
    private final Long groupId;
    private final long timestamp;

    protected ArchiveEvent(ArchiveEventType type, Long taskId, Long groupId) {
        this.eventId = UUID.randomUUID().toString();
        this.type = type;
        this.taskId = taskId;
        this.groupId = groupId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getEventId() { return eventId; }
    public ArchiveEventType getType() { return type; }
    public Long getTaskId() { return taskId; }
    public Long getGroupId() { return groupId; }
    public long getTimestamp() { return timestamp; }
}
