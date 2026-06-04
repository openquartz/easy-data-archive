package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

@Data
public class ArchiveGroupTaskStatsView {

    private Long totalCount;
    private Long successCount;
    private Long failedCount;
    private Long runningCount;
    private Integer lastExecuteStatus;
    private Long lastExecuteTime;
}
