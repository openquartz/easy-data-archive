package com.openquartz.easyarchive.common.enums;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BinarySwitchEnum {

    ON(0, "是"),
    OFF(1, "否");

    private final Integer code;
    private final String desc;

    BinarySwitchEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static BinarySwitchEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }

    public static boolean isOn(Integer code) {
        return ON.code.equals(code);
    }

    public static boolean isOff(Integer code) {
        return OFF.code.equals(code);
    }
}
