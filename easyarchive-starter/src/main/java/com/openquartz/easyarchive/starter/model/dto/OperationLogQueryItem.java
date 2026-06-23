package com.openquartz.easyarchive.starter.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class OperationLogQueryItem {

    private Long id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date operateTime;

    private String operator;

    private String moduleCode;

    private String actionCode;

    private String buttonName;

    private String bizIdentifier;

    private String content;

    private Integer resultStatus;

    private String errorMessage;
}
