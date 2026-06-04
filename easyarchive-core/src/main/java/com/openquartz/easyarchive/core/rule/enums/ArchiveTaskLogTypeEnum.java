package com.openquartz.easyarchive.core.rule.enums;

import java.util.Arrays;

public enum ArchiveTaskLogTypeEnum {

    START("START", "开始"),
    FINISH("FINISH", "完成"),
    PROGRESS("PROGRESS", "进度"),
    CANCEL("CANCEL", "取消"),
    ERROR("ERROR", "错误");

    private final String code;
    private final String desc;

    ArchiveTaskLogTypeEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ArchiveTaskLogTypeEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}
