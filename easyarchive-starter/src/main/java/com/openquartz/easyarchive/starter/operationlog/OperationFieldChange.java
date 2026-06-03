package com.openquartz.easyarchive.starter.operationlog;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OperationFieldChange {

    private String field;

    private String label;

    private String beforeValue;

    private String afterValue;
}
