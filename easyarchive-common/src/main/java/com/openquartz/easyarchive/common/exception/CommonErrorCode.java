package com.openquartz.easyarchive.common.exception;

import lombok.Getter;

/**
 * @author svnee
 **/
@Getter
public enum CommonErrorCode implements EasyArchiveErrorCode {

    PARAM_ILLEGAL_ERROR("01", "参数不合法异常"),
    CLASS_NOT_FOUND_ERROR("02", "Class {0} not exist!", true),
    METHOD_NOT_EXIST_ERROR("03", "Method not exist!"),
    SEQUENCE_EXECUTOR_REGISTERED_ERROR("04", "sequence:{0} not registered!", true);

    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    private static final String BASE_CODE = "CommonError-";

    CommonErrorCode(String errorCode, String errorMsg) {
        this(errorCode, errorMsg, false);
    }

    CommonErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
