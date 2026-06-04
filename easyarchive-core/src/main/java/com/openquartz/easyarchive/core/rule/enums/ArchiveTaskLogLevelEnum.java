package com.openquartz.easyarchive.core.rule.enums;

import java.util.Arrays;

public enum ArchiveTaskLogLevelEnum {

    INFO("INFO", "信息"),
    WARN("WARN", "警告"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String desc;

    ArchiveTaskLogLevelEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ArchiveTaskLogLevelEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
