package com.openquartz.easyarchive.core.rule.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.openquartz.easyarchive.common.entity.BaseEntity;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskExecutePhaseEnum;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskLogLevelEnum;
import com.openquartz.easyarchive.core.rule.enums.ArchiveTaskLogTypeEnum;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
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

    public void setLogType(ArchiveTaskLogTypeEnum logType) {
        this.logType = logType == null ? null : logType.getCode();
    }

    public void setLogLevel(ArchiveTaskLogLevelEnum logLevel) {
        this.logLevel = logLevel == null ? null : logLevel.getCode();
    }

    public void setExecutePhase(ArchiveTaskExecutePhaseEnum executePhase) {
        this.executePhase = executePhase == null ? null : executePhase.getCode();
    }
}
