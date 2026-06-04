package com.openquartz.easyarchive.core.rule.enums;

import java.util.Arrays;

public enum ArchiveTaskExecutePhaseEnum {

    TASK_START("TASK_START", "任务开始"),
    RULE_START("RULE_START", "规则开始"),
    RULE_END("RULE_END", "规则结束"),
    TASK_PROGRESS("TASK_PROGRESS", "任务进度"),
    TASK_END("TASK_END", "任务结束");

    private final String code;
    private final String desc;

    ArchiveTaskExecutePhaseEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ArchiveTaskExecutePhaseEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
