package com.openquartz.easyarchive.starter.exception;

import com.openquartz.easyarchive.common.exception.EasyArchiveErrorCode;
import com.openquartz.easyarchive.common.exception.EasyArchiveException;

public class StarterManageException extends EasyArchiveException {

    public StarterManageException(EasyArchiveErrorCode errorCode) {
        super(errorCode);
    }

    public StarterManageException(EasyArchiveErrorCode errorCode, String errorMsg) {
        super(errorCode, errorMsg);
    }

    public static StarterManageException withPlaceholders(EasyArchiveErrorCode errorCode, Object... placeHold) {
        EasyArchiveException exception = EasyArchiveException.withPlaceholders(errorCode, placeHold);
        return new StarterManageException(errorCode, exception.getMessage());
    }
}
