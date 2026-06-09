package com.openquartz.easyarchive.starter.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 任务日志 VO
 * 仅用于 API 响应，时间字段格式化为东八区 yyyy-MM-dd HH:mm:ss
 */
@Data
public class TaskLogVO {

    private Long id;

    private Long taskId;

    private String logType;

    private String logLevel;

    private String logContent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date logTime;

    private Long processedCount;

    private BigDecimal processSpeed;

    private String executePhase;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
}
