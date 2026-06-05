package com.openquartz.easyarchive.core.event;

import lombok.Getter;

@Getter
public class RuleStartEvent extends ArchiveEvent {

    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;

    public RuleStartEvent(Long taskId,
                          Long groupId,
                          String sourceTable,
                          String targetTable,
                          String ruleType) {

        super(ArchiveEventType.RULE_START, taskId, groupId);
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.ruleType = ruleType;
    }

}
