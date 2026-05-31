package com.openquartz.easyarchive.core.event;

public class RuleStartEvent extends ArchiveEvent {

    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;

    public RuleStartEvent(Long taskId, Long groupId,
                          String sourceTable, String targetTable,
                          String ruleType) {
        super(ArchiveEventType.RULE_START, taskId, groupId);
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.ruleType = ruleType;
    }

    public String getSourceTable() { return sourceTable; }
    public String getTargetTable() { return targetTable; }
    public String getRuleType() { return ruleType; }
}
