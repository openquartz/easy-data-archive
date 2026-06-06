package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class InAppNotificationListItem {

    private Long notificationId;

    private String title;

    private String summary;

    private String taskStatus;

    private Long groupId;

    private String groupName;

    private Long taskId;

    private Integer readStatus;

    private Date createdTime;
}
