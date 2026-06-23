package com.openquartz.easyarchive.starter.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveGroupView extends ArchiveGroup {

    private String ownerDisplayName;
    private Long activeTaskId;
    private Integer activeTaskStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date activeTaskStartTime;
    private Long activeTaskProcessedRecords;
    private BigDecimal activeTaskProcessedSpeed;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date activeTaskHeartbeatTime;
    private Boolean canTrigger;
    private Boolean canCancelActiveTask;
    private Boolean canViewActiveTask;
}
