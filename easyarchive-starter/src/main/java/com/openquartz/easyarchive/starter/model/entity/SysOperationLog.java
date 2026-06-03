package com.openquartz.easyarchive.starter.model.entity;

import lombok.Data;

import java.util.Date;

@Data
public class SysOperationLog {

    private Long id;

    private Long userId;

    private String moduleCode;

    private String actionCode;

    private String buttonName;

    private String bizType;

    private Long bizId;

    private String bizKey;

    private String content;

    private String requestUri;

    private String requestMethod;

    private String requestParam;

    private String responseCode;

    private Integer resultStatus;

    private Long costMs;

    private String clientIp;

    private String errorMessage;

    private Date operateTime;

    private Date createdTime;
}
