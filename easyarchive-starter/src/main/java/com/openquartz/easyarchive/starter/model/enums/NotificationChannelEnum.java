package com.openquartz.easyarchive.starter.model.enums;

public enum NotificationChannelEnum {
    FEISHU,
    WECOM;

    public static boolean supports(String value) {
        if (value == null) {
            return false;
        }
        for (NotificationChannelEnum channel : values()) {
            if (channel.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }
}
