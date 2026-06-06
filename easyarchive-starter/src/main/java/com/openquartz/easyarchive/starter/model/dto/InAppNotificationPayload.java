package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

@Data
public class InAppNotificationPayload {

    private Long groupId;

    private String groupCode;

    private String groupName;

    private Long taskId;

    private String taskStatus;

    private Long totalRows;

    private Long elapsedMs;

    private Long sourceTimestamp;
}
