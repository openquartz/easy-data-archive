package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 归档规则实体
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class EaArchiveRule {

    private Long id;
    private Long groupId;
    private String ruleCode;
    private String ruleName;
    private String ruleType;
    private Integer priorityNo;
    private String sourceTable;
    private String targetTable;
    private String idColumn;
    private String fetchSqlTemplate;
    private String deleteWhere;
    private String startExpr;
    private String endExpr;
    private Integer keepDays;
    private Integer stepCount;
    private Integer stepRounds;
    private Integer pauseMs;
    private Integer enableWrite;
    private Integer enableClean;
    private Integer enableStatus;
    private Integer lastCheckStatus;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
    private Long creatorId;
    private Long updaterId;
    private Integer deleted;

}