package com.openquartz.easyarchive.core.expr.executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.common.exception.EasyArchiveException;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import org.junit.jupiter.api.Test;

class HashModExecutorTest {

    private final HashModExecutor executor = new HashModExecutor();

    @Test
    void shouldHashSingleValueAndApplyModulo() {
        Result result = executor.exec(Command.parse("hash_mod 16 abc"));

        assertEquals("2", result.getOutput());
    }

    @Test
    void shouldJoinAllDataParamsBeforeHashing() {
        Result result = executor.exec(Command.parse("hash_mod 16 ab c"));

        assertEquals("2", result.getOutput());
    }

    @Test
    void shouldNormalizeNegativeHashResult() {
        Result result = executor.exec(Command.parse("hash_mod 10 polygenelubricants"));

        assertEquals("2", result.getOutput());
    }

    @Test
    void shouldRejectNonPositiveModulo() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("hash_mod 0 abc")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }

    @Test
    void shouldRejectNonNumericModulo() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("hash_mod abc value")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }

    @Test
    void shouldRejectMissingDataParams() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("hash_mod 16")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }
}
