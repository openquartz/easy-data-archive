package com.openquartz.easyarchive.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EasyArchiveExceptionTest {

    @Test
    void shouldCreatePlaceholderMessageWithoutThrowingFromFactoryMethod() {
        EasyArchiveException exception = EasyArchiveException.withPlaceholders(
            CommonErrorCode.CLASS_NOT_FOUND_ERROR, "demo.Type");

        assertEquals(CommonErrorCode.CLASS_NOT_FOUND_ERROR, exception.getErrorCode());
        assertEquals("Class demo.Type not exist!", exception.getMessage());
    }

    @Test
    void shouldThrowCommonErrorCodeThroughAsserts() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> Asserts.notNull(null, CommonErrorCode.PARAM_ILLEGAL_ERROR));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }
}
