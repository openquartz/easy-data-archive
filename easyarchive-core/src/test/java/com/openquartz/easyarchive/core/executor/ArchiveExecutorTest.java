package com.openquartz.easyarchive.core.executor;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ArchiveExecutorTest {

    @Test
    void shouldSaveTimeRuleRangeLog() {
        ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
        ArchiveExecutor executor = new ArchiveExecutor(
            null, null, null, null, 101L, 11L, null, archiveLogRepository);

        executor.logTimeRuleRange("archive_order_202606", new Date(0L), new Date(3600_000L));

        ArchiveTaskLog log = captureSavedTaskLog(archiveLogRepository);
        assertEquals(101L, log.getTaskId());
        assertEquals("RULE_START", log.getExecutePhase());
        assertEquals("START", log.getLogType());
        assertEquals("INFO", log.getLogLevel());
        assertTrue(log.getLogContent().contains("规则范围:archive_order_202606"));
        assertTrue(log.getLogContent().contains("时间范围:1970-01-01 08:00:00 -> 1970-01-01 09:00:00"));
    }

    @Test
    void shouldSaveIdRuleRangeLog() {
        ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
        ArchiveExecutor executor = new ArchiveExecutor(
            null, null, null, null, 102L, 12L, null, archiveLogRepository);

        executor.logIdRuleRange("archive_order_202606", 10L, 1000L);

        ArchiveTaskLog log = captureSavedTaskLog(archiveLogRepository);
        assertEquals(102L, log.getTaskId());
        assertEquals("RULE_START", log.getExecutePhase());
        assertEquals("START", log.getLogType());
        assertEquals("INFO", log.getLogLevel());
        assertTrue(log.getLogContent().contains("规则范围:archive_order_202606"));
        assertTrue(log.getLogContent().contains("ID范围:10 -> 1000"));
    }

    @Test
    void shouldAdvanceTimeWindowByConfiguredStepMinutes() {
        ArchiveGroupItemByTime rule = new ArchiveGroupItemByTime();
        rule.setStepMinutes(30);

        Date next = ArchiveExecutor.resolveTimeWindowEnd(new Date(0L), rule);

        assertEquals(30L * 60L * 1000L, next.getTime());
    }

    private static ArchiveTaskLog captureSavedTaskLog(ArchiveLogRepository repository) {
        ArgumentCaptor<ArchiveTaskLog> captor = ArgumentCaptor.forClass(ArchiveTaskLog.class);
        verify(repository).saveTaskLog(captor.capture());
        return captor.getValue();
    }
}
