package com.openquartz.easyarchive.common.enums;

import java.util.Arrays;

public enum DatasourceStatusEnum {

    UNTESTED(0, "未测试"),
    ENABLED(1, "已启用"),
    DISABLED(2, "已停用");

    private final Integer code;
    private final String desc;

    DatasourceStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static DatasourceStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isEnabled(Integer code) {
        return ENABLED.code.equals(code);
    }
}
