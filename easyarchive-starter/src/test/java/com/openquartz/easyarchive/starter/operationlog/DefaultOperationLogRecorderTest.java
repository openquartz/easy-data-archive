package com.openquartz.easyarchive.starter.operationlog;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultOperationLogRecorderTest {

    @Test
    void shouldWriteCommandIntoCurrentContext() {
        OperationLogContext context = new OperationLogContext();
        OperationLogContextHolder.set(context);
        try {
            DefaultOperationLogRecorder recorder = new DefaultOperationLogRecorder();
            OperationLogCommand command = new OperationLogCommand(
                    "ARCHIVE_GROUP", "UPDATE", "保存分组", "ARCHIVE_GROUP", 10L, "ORDER_ARCHIVE",
                    "\"分组名称\" 从 \"旧分组\" 修改为：\"新分组\"", Collections.emptyList());

            recorder.record(command);

            assertEquals("保存分组", context.getButtonName());
            assertEquals("ARCHIVE_GROUP", context.getBizType());
            assertEquals("\"分组名称\" 从 \"旧分组\" 修改为：\"新分组\"", context.getContent());
        } finally {
            OperationLogContextHolder.clear();
        }
    }
}
