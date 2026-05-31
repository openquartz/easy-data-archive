package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 归档任务实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EaArchiveTask {

    private Long id;
    private String taskNo;
    private Long groupId;
    private String triggerType;
    private String executeStatus;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long processedRecords;
    private Double processedSpeed;
    private Long currentRuleId;
    private LocalDateTime heartbeatTime;
    private String cancelReason;
    private String errorMsg;
    private Long triggerUserId;
    private Long finishedFlag;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;

}