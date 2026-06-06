package com.openquartz.easyarchive.core.expr.executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.openquartz.easyarchive.common.exception.CommonErrorCode;
import com.openquartz.easyarchive.common.exception.EasyArchiveException;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import org.junit.jupiter.api.Test;

class ModExecutorTest {

    private final ModExecutor executor = new ModExecutor();

    @Test
    void shouldApplyModuloForPositiveIntegers() {
        Result result = executor.exec(Command.parse("mod 16 33"));

        assertEquals("1", result.getOutput());
    }

    @Test
    void shouldRejectNonPositiveModulo() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("mod 0 33")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }

    @Test
    void shouldRejectNonPositiveSourceNumber() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("mod 16 0")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }

    @Test
    void shouldRejectNonNumericSourceNumber() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("mod 16 abc")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }

    @Test
    void shouldRejectMissingSourceNumber() {
        EasyArchiveException exception = assertThrows(EasyArchiveException.class,
            () -> executor.validate(Command.parse("mod 16")));

        assertEquals(CommonErrorCode.PARAM_ILLEGAL_ERROR, exception.getErrorCode());
    }
}
