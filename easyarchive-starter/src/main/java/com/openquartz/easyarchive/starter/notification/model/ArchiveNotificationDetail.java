package com.openquartz.easyarchive.starter.notification.model;

import lombok.Data;

@Data
public class ArchiveNotificationDetail {

    private String sourceTable;
    private String targetTable;
    private Long processedRows;
    private Long elapsedMs;
    private String status;
    private String reason;
}
