package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.util.Date;

@Data
public class OperationLogQueryItem {

    private Long id;

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
