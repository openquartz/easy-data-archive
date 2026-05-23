package com.openquartz.easyarchive.common.exception;

import java.text.MessageFormat;

/**
 * 异步文件异常
 *
 * @author svnee
 */
public class EasyArchiveException extends RuntimeException {

    private final transient EasyArchiveErrorCode errorCode;

    public EasyArchiveException(EasyArchiveErrorCode errorCode) {
        super(errorCode.getErrorMsg());
        this.errorCode = errorCode;
    }

    public EasyArchiveException(EasyArchiveErrorCode errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
    }

    public EasyArchiveErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * 替换占位符号
     *
     * @param placeHold 占位
     * @return 异常
     */
    public static EasyArchiveException replacePlaceHold(EasyArchiveErrorCode errorCode, Object... placeHold) {
        throw new EasyArchiveException(errorCode, MessageFormat.format(errorCode.getErrorMsg(), placeHold));
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
