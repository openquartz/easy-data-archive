package com.openquartz.easyarchive.starter.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 归档任务 VO
 * 仅用于 API 响应，时间字段格式化为东八区 yyyy-MM-dd HH:mm:ss
 */
@Data
public class TaskVO {

    private Long id;

    private Long groupId;

    private String groupName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    private Integer executeStatus;

    private String errorMsg;

    private Long processedRecords;

    private BigDecimal processedSpeed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date heartbeatTime;

    private Long finishedFlag;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updatedTime;
}
