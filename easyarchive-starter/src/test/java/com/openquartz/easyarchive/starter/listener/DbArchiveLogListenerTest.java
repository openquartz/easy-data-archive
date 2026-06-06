package com.openquartz.easyarchive.starter.listener;

import com.openquartz.easyarchive.core.event.TaskEndEvent;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DbArchiveLogListenerTest {

    @Test
    void shouldSetFinishedFlagToTaskIdWhenTaskEndsAsCancelled() {
        ArchiveLogRepository repository = mock(ArchiveLogRepository.class);
        DbArchiveLogListener listener = new DbArchiveLogListener(repository);

        listener.onEvent(new TaskEndEvent(12L, 3L, false, 10L, 100L, "cancelled", true));

        ArchiveGroupExecuteTask task = captureUpdatedTask(repository);
        ArchiveTaskLog taskLog = captureSavedTaskLog(repository);
        assertEquals(ArchiveGroupExecuteTask.STATUS_CANCELLED, task.getExecuteStatus());
        assertEquals(12L, task.getFinishedFlag());
        assertEquals(enumCode("ArchiveTaskLogTypeEnum", "CANCEL"), taskLog.getLogType());
        assertEquals(enumCode("ArchiveTaskLogLevelEnum", "WARN"), taskLog.getLogLevel());
        assertEquals(enumCode("ArchiveTaskExecutePhaseEnum", "TASK_END"), taskLog.getExecutePhase());
    }

    @Test
    void shouldSetFinishedFlagToTaskIdWhenTaskEndsAsFailed() {
        ArchiveLogRepository repository = mock(ArchiveLogRepository.class);
        DbArchiveLogListener listener = new DbArchiveLogListener(repository);

        listener.onEvent(new TaskEndEvent(13L, 4L, false, 8L, 200L, "boom"));

        ArchiveGroupExecuteTask task = captureUpdatedTask(repository);
        assertEquals(ArchiveGroupExecuteTask.STATUS_FAILED, task.getExecuteStatus());
        assertEquals(13L, task.getFinishedFlag());
    }

    private static ArchiveGroupExecuteTask captureUpdatedTask(ArchiveLogRepository repository) {
        ArgumentCaptor<ArchiveGroupExecuteTask> captor = ArgumentCaptor.forClass(ArchiveGroupExecuteTask.class);
        verify(repository).updateTaskExecution(captor.capture());
        verify(repository).saveTaskLog(any());
        return captor.getValue();
    }

    private static ArchiveTaskLog captureSavedTaskLog(ArchiveLogRepository repository) {
        ArgumentCaptor<ArchiveTaskLog> captor = ArgumentCaptor.forClass(ArchiveTaskLog.class);
        verify(repository).saveTaskLog(captor.capture());
        return captor.getValue();
    }

    private static String enumCode(String simpleClassName, String constantName) {
        try {
            Class<?> enumClass = Class.forName("com.openquartz.easyarchive.core.rule.enums." + simpleClassName);
            Enum<?> constant = Enum.valueOf(enumClass.asSubclass(Enum.class), constantName);
            return (String) enumClass.getMethod("getCode").invoke(constant);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("枚举定义缺失: " + simpleClassName + "." + constantName, exception);
        }
    }
}
