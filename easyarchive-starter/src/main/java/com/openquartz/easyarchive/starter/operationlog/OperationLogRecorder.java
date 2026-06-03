package com.openquartz.easyarchive.starter.operationlog;

public interface OperationLogRecorder {

    void record(OperationLogCommand command);

    void recordFailure(String errorMessage);
}
