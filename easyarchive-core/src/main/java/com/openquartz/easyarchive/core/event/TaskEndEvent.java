package com.openquartz.easyarchive.core.event;

public class TaskEndEvent extends ArchiveEvent {

    private final boolean success;
    private final long totalRows;
    private final long elapsedMs;
    private final String errorMsg;
    private final boolean cancelled;

    public TaskEndEvent(Long taskId, Long groupId,
                        boolean success, long totalRows,
                        long elapsedMs, String errorMsg) {
        this(taskId, groupId, success, totalRows, elapsedMs, errorMsg, false);
    }

    public TaskEndEvent(Long taskId, Long groupId,
                        boolean success, long totalRows,
                        long elapsedMs, String errorMsg, boolean cancelled) {
        super(ArchiveEventType.TASK_END, taskId, groupId);
        this.success = success;
        this.totalRows = totalRows;
        this.elapsedMs = elapsedMs;
        this.errorMsg = errorMsg;
        this.cancelled = cancelled;
    }

    public boolean isSuccess() { return success; }
    public long getTotalRows() { return totalRows; }
    public long getElapsedMs() { return elapsedMs; }
    public String getErrorMsg() { return errorMsg; }
    public boolean isCancelled() { return cancelled; }
}
