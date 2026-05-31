package com.openquartz.easyarchive.core.event;

public class RuleEndEvent extends ArchiveEvent {

    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;
    private final boolean success;
    private final long processedRows;
    private final long elapsedMs;
    private final String errorMsg;

    public RuleEndEvent(Long taskId, Long groupId,
                        String sourceTable, String targetTable,
                        String ruleType, boolean success,
                        long processedRows, long elapsedMs,
                        String errorMsg) {
        super(ArchiveEventType.RULE_END, taskId, groupId);
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.ruleType = ruleType;
        this.success = success;
        this.processedRows = processedRows;
        this.elapsedMs = elapsedMs;
        this.errorMsg = errorMsg;
    }

    public String getSourceTable() { return sourceTable; }
    public String getTargetTable() { return targetTable; }
    public String getRuleType() { return ruleType; }
    public boolean isSuccess() { return success; }
    public long getProcessedRows() { return processedRows; }
    public long getElapsedMs() { return elapsedMs; }
    public String getErrorMsg() { return errorMsg; }
}
