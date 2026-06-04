package com.openquartz.easyarchive.common.enums;

import java.util.Arrays;

public enum ArchiveTaskStatusEnum {

    WAITING(0, "等待"),
    RUNNING(1, "运行中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败"),
    CANCELLING(4, "取消中"),
    CANCELLED(5, "已取消");

    private final Integer code;
    private final String desc;

    ArchiveTaskStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ArchiveTaskStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isActive(Integer code) {
        return WAITING.code.equals(code)
                || RUNNING.code.equals(code)
                || CANCELLING.code.equals(code);
    }

    public static boolean isTerminal(Integer code) {
        return SUCCESS.code.equals(code)
                || FAILED.code.equals(code)
                || CANCELLED.code.equals(code);
    }
}
