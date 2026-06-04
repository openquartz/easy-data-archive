package com.openquartz.easyarchive.core.exception;

import com.openquartz.easyarchive.common.exception.EasyArchiveErrorCode;
import lombok.Getter;

@Getter
public enum CoreErrorCode implements EasyArchiveErrorCode {

    RANDOM_ALPHA_NUM_TYPE_UNSUPPORTED("01", "Unsupported random alpha num type: {0}", true),
    TIME_UNIT_UNSUPPORTED("02", "Unsupported time unit: {0}", true);

    private static final String BASE_CODE = "CoreError-";

    private final String errorCode;
    private final String errorMsg;
    private final boolean replacePlaceHold;

    CoreErrorCode(String errorCode, String errorMsg, boolean replacePlaceHold) {
        this.errorCode = BASE_CODE + errorCode;
        this.errorMsg = errorMsg;
        this.replacePlaceHold = replacePlaceHold;
    }
}
