package com.openquartz.easyarchive.core.event;

public class TaskStartEvent extends ArchiveEvent {

    private final int ruleCount;

    public TaskStartEvent(Long taskId, Long groupId, int ruleCount) {
        super(ArchiveEventType.TASK_START, taskId, groupId);
        this.ruleCount = ruleCount;
    }

    public int getRuleCount() { return ruleCount; }
}
