package com.openquartz.easyarchive.starter.operationlog;

import lombok.Data;

@Data
public class OperationLogContext {

    private String moduleCode;

    private String actionCode;

    private String buttonName;

    private String bizType;

    private Long bizId;

    private String bizKey;

    private String content;

    private String requestParamSummary;

    private String responseCode;

    private Integer resultStatus;

    private String errorMessage;
}
