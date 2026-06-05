package com.openquartz.easyarchive.starter.model.dto;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class ArchiveGroupView extends ArchiveGroup {

    private Long activeTaskId;
    private Integer activeTaskStatus;
    private Date activeTaskStartTime;
    private Long activeTaskProcessedRecords;
    private BigDecimal activeTaskProcessedSpeed;
    private Date activeTaskHeartbeatTime;
    private Boolean canTrigger;
    private Boolean canCancelActiveTask;
    private Boolean canViewActiveTask;
}
