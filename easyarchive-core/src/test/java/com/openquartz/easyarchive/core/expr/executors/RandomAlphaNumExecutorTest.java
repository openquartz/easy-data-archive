package com.openquartz.easyarchive.core.expr.executors;

import com.openquartz.easyarchive.common.exception.EasyArchiveException;
import com.openquartz.easyarchive.core.exception.CoreErrorCode;
import com.openquartz.easyarchive.core.expr.cmd.Command;
import com.openquartz.easyarchive.core.expr.cmd.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomAlphaNumExecutorTest {

    private final RandomAlphaNumExecutor executor = new RandomAlphaNumExecutor();

    @Test
    void shouldGenerateUppercaseWithoutIForConfiguredType() {
        Result result = executor.exec(Command.parse("rand_c 8 UPPERCASE_NO_I"));

        String value = result.getOutput();
        assertEquals(8, value.length());
        assertTrue(value.chars().allMatch(ch -> ch >= 'A' && ch <= 'Z'));
        assertFalse(value.contains("I"));
    }

    @Test
    void shouldRejectUnsupportedClassifierWithErrorCode() {
        Command command = Command.parse("rand_c 8 BAD_TYPE");

        EasyArchiveException exception = assertThrows(EasyArchiveException.class, () -> executor.exec(command));

        assertEquals(CoreErrorCode.RANDOM_ALPHA_NUM_TYPE_UNSUPPORTED, exception.getErrorCode());
    }
}
