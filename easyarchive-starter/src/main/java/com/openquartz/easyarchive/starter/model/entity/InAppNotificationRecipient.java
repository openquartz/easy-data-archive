package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class InAppNotificationRecipient {

    private Long id;

    private Long notificationId;

    private Long recipientUserId;

    private Integer readStatus;

    private Date readTime;

    private Integer deliveryStatus;

    private Date createdTime;
}
