package com.openquartz.easyarchive.starter.operationlog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OperationLogContextHolderTest {

    @Test
    void shouldStoreAndClearContextPerThread() {
        OperationLogContext context = new OperationLogContext();
        context.setModuleCode("ARCHIVE_GROUP");

        OperationLogContextHolder.set(context);

        assertSame(context, OperationLogContextHolder.get());

        OperationLogContextHolder.clear();

        assertNull(OperationLogContextHolder.get());
    }
}
