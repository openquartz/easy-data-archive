package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class InAppNotification {

    private Long id;

    private String bizType;

    private Long bizId;

    private String category;

    private String level;

    private Long groupId;

    private String groupName;

    private Long taskId;

    private String taskStatus;

    private String title;

    private String contentSummary;

    private String payloadJson;

    private Date sourceTime;

    private Date createdTime;
}
