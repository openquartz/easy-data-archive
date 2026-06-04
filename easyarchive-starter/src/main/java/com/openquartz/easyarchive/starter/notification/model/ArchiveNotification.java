package com.openquartz.easyarchive.starter.notification.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ArchiveNotification {

    private Long taskId;
    private String status;
    private String startedAt;
    private String endedAt;
    private String groupCode;
    private String groupName;
    private String groupRemark;
    private Long totalRows;
    private String ownerName;
    private String reason;
    private List<ArchiveNotificationDetail> details = new ArrayList<>();
}
