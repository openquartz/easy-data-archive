package com.openquartz.easyarchive.core.exception;

public class TaskCancelledException extends RuntimeException {

    private final Long taskId;

    public TaskCancelledException(Long taskId) {
        super("Task cancelled: " + taskId);
        this.taskId = taskId;
    }

    public TaskCancelledException(Long taskId, String reason) {
        super("Task cancelled: " + taskId + ", reason: " + reason);
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}
