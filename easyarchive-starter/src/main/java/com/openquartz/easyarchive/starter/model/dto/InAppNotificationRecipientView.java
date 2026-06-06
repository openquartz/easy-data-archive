package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class InAppNotificationRecipientView {

    private Long notificationId;

    private Integer readStatus;

    private Date createdTime;
}
