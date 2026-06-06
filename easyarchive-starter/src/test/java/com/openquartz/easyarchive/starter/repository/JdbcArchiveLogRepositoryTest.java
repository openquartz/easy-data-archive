package com.openquartz.easyarchive.starter.repository;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveTaskLogMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class JdbcArchiveLogRepositoryTest {

    @Test
    void shouldSetFinishedFlagWhenUpdatingTerminalTaskStatus() {
        ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
        ArchiveTaskLogMapper taskLogMapper = mock(ArchiveTaskLogMapper.class);
        JdbcArchiveLogRepository repository = new JdbcArchiveLogRepository(taskMapper, taskLogMapper);

        repository.updateTaskStatus(15L, ArchiveGroupExecuteTask.STATUS_CANCELLED);

        ArgumentCaptor<ArchiveGroupExecuteTask> captor = ArgumentCaptor.forClass(ArchiveGroupExecuteTask.class);
        verify(taskMapper).update(captor.capture());
        verify(taskMapper, never()).updateExecuteStatus(15L, ArchiveGroupExecuteTask.STATUS_CANCELLED);
        assertEquals(15L, captor.getValue().getId());
        assertEquals(ArchiveGroupExecuteTask.STATUS_CANCELLED, captor.getValue().getExecuteStatus());
        assertEquals(15L, captor.getValue().getFinishedFlag());
    }
}
