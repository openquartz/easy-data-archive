package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

/**
 * Merged archive group item summary for ID and TIME strategies.
 */
@Data
public class ArchiveGroupItemSummary {

    private String itemType;
    private Long id;
    private Long groupId;
    private String sourceTable;
    private String targetTable;
    private Integer priority;
    private Integer stepCount;
    private Integer enableWrite;
    private Integer enableClean;
    private Integer enableStatus;
    private String rangeStart;
    private String rangeEnd;
}
