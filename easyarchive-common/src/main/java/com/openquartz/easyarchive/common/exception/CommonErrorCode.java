package com.openquartz.easyarchive.common.exception;

import lombok.Getter;

/**
 * @author svnee
 **/
@Getter
public enum CommonErrorCode implements EasyArchiveErrorCode {

    PARAM_ILLEGAL_ERROR("01", "Param illegal!"),
    METHOD_NOT_EXIST_ERROR("02", "Method not exist!"),
    SEQUENCE_EXECUTOR_REGISTERED_ERROR("03","sequence:{0} not registered!" ),
    ;
    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    private static final String SIMPLE_BASE_CODE = "CommonError-";

    CommonErrorCode(String errorCode, String errorMsg) {
        this(errorCode, errorMsg, false);
    }

    CommonErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = SIMPLE_BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
