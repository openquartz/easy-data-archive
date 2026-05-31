package com.openquartz.easyarchive.core.rule.entity;

import com.openquartz.easyarchive.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 执行日志
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ArchiveTaskLog extends BaseEntity {

    private Long id;

    private Long taskId;

    /**
     * 日志类型
     */
    private String logType;

    /**
     * 日志级别
     */
    private String logLevel;

    /**
     * 日志内容
     */
    private String logContent;

    /**
     * 日志时间
     */
    private Date logTime;

    /**
     * 已处理数
     */
    private Long processedCount;

    /**
     * 处理速度
     */
    private java.math.BigDecimal processSpeed;

    /**
     * 执行阶段
     */
    private String executePhase;
}

