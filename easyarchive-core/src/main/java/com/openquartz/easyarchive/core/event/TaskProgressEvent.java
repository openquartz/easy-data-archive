package com.openquartz.easyarchive.core.event;

public class TaskProgressEvent extends ArchiveEvent {

    private final long processedRecords;
    private final long elapsedMs;
    private final Long currentRuleId;
    private final String sourceTable;

    public TaskProgressEvent(Long taskId, Long groupId,
                             long processedRecords, long elapsedMs,
                             Long currentRuleId, String sourceTable) {
        super(ArchiveEventType.TASK_PROGRESS, taskId, groupId);
        this.processedRecords = processedRecords;
        this.elapsedMs = elapsedMs;
        this.currentRuleId = currentRuleId;
        this.sourceTable = sourceTable;
    }

    public long getProcessedRecords() { return processedRecords; }
    public long getElapsedMs() { return elapsedMs; }
    public Long getCurrentRuleId() { return currentRuleId; }
    public String getSourceTable() { return sourceTable; }
}
