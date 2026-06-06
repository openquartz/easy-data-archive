package com.openquartz.easyarchive.starter.model.enums;

public enum InAppNotificationReadStatusEnum {
    UNREAD(0),
    READ(1);

    private final int code;

    InAppNotificationReadStatusEnum(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
