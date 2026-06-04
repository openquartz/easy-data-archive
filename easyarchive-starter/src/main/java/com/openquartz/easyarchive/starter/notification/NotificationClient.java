package com.openquartz.easyarchive.starter.notification;

import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;

public interface NotificationClient {

    NotificationChannelEnum getChannel();

    void send(String webhookUrl, String message);
}
