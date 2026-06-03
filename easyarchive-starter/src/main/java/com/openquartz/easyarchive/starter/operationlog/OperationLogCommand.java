package com.openquartz.easyarchive.starter.operationlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationLogCommand {

    private String moduleCode;

    private String actionCode;

    private String buttonName;

    private String bizType;

    private Long bizId;

    private String bizKey;

    private String content;

    private List<OperationFieldChange> changes;
}
