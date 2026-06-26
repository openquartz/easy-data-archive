package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveTaskOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveTaskLogServiceImplTest {

    private final ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveResourceAccessService archiveResourceAccessService = mock(ArchiveResourceAccessService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final ArchiveTaskOperationLogPresenter presenter = mock(ArchiveTaskOperationLogPresenter.class);
    private final OperationLogRecorder recorder = mock(OperationLogRecorder.class);
    private final ArchiveTaskGroupNameResolver groupNameResolver = mock(ArchiveTaskGroupNameResolver.class);
    private final ArchiveTaskLogServiceImpl service = new ArchiveTaskLogServiceImpl(
            archiveLogRepository, taskMapper, archiveResourceAccessService, currentUserService, presenter, recorder,
            groupNameResolver);

    @Test
    void shouldUseAuthorizedTaskPageForNormalUser() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser());
        when(taskMapper.selectPageByUser(2L, 0, 20, "1", null)).thenReturn(Collections.emptyList());
        when(taskMapper.countByUser(2L, "1", null)).thenReturn(0);

        Map<String, Object> result = service.queryTasks(1, 20, "1", null);

        assertEquals(0, result.get("total"));
        verify(taskMapper).selectPageByUser(2L, 0, 20, "1", null);
    }

    @Test
    void shouldCheckTaskPermissionBeforeCancel() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(11L);
        task.setGroupId(21L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        when(archiveLogRepository.queryTaskById(11L)).thenReturn(task);
        when(presenter.buildCancel(task, "stop", "取消中")).thenReturn(command("CANCEL"));

        service.cancelTask(11L, "stop");

        verify(archiveResourceAccessService).assertTaskAccessible(11L);
        verify(archiveLogRepository).updateTaskStatus(11L, ArchiveGroupExecuteTask.STATUS_CANCELLING);
        verify(recorder).record(any());
    }

    @Test
    void shouldRecordCancelledResultForWaitingTask() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(12L);
        task.setGroupId(22L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_WAITING);
        when(archiveLogRepository.queryTaskById(12L)).thenReturn(task);
        when(presenter.buildCancel(task, "manual", "已取消")).thenReturn(command("CANCEL"));

        service.cancelTask(12L, "manual");

        ArchiveTaskLog log = captureSavedTaskLog();
        verify(archiveResourceAccessService).assertTaskAccessible(12L);
        verify(archiveLogRepository).updateTaskStatus(12L, ArchiveGroupExecuteTask.STATUS_CANCELLED);
        verify(recorder).record(any());
        assertEquals(enumCode("ArchiveTaskLogTypeEnum", "CANCEL"), log.getLogType());
        assertEquals(enumCode("ArchiveTaskLogLevelEnum", "WARN"), log.getLogLevel());
        assertEquals(enumCode("ArchiveTaskExecutePhaseEnum", "TASK_END"), log.getExecutePhase());
    }

    @Test
    void shouldRecordDuplicateCancelResultWithoutUpdatingTask() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(13L);
        task.setGroupId(23L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLING);
        when(archiveLogRepository.queryTaskById(13L)).thenReturn(task);
        when(presenter.buildCancel(task, "manual", "重复请求，无需处理")).thenReturn(command("CANCEL"));

        service.cancelTask(13L, "manual");

        verify(archiveResourceAccessService).assertTaskAccessible(13L);
        verify(archiveLogRepository, never()).updateTaskStatus(13L, ArchiveGroupExecuteTask.STATUS_CANCELLING);
        verify(recorder).record(any());
    }

    @Test
    void shouldRecordFailureBeforeThrowingForTerminalTask() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(14L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLED);
        when(archiveLogRepository.queryTaskById(14L)).thenReturn(task);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.cancelTask(14L, "manual"));
        assertEquals(StarterErrorCode.ARCHIVE_TASK_TERMINAL_CANNOT_CANCEL, error.getErrorCode());

        verify(archiveResourceAccessService).assertTaskAccessible(14L);
        verify(recorder).recordFailure("任务已结束，无法取消");
    }

    @Test
    void shouldRecordCleanupOperation() {
        when(archiveLogRepository.deleteByRetentionDays(30)).thenReturn(8);
        when(presenter.buildCleanup(30, 8)).thenReturn(command("CLEANUP"));

        assertEquals(8, service.cleanup(30));

        verify(recorder).record(any());
    }

    private static CurrentUserInfo currentUser() {
        CurrentUserInfo currentUserInfo = new CurrentUserInfo();
        currentUserInfo.setUserId(2L);
        currentUserInfo.setRoleCode("normal_user");
        return currentUserInfo;
    }

    private ArchiveTaskLog captureSavedTaskLog() {
        ArgumentCaptor<ArchiveTaskLog> captor = ArgumentCaptor.forClass(ArchiveTaskLog.class);
        verify(archiveLogRepository).saveTaskLog(captor.capture());
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

    private static OperationLogCommand command(String action) {
        return new OperationLogCommand("ARCHIVE_TASK", action, action, "ARCHIVE_TASK", 1L, "TASK_1", action, null);
    }
}
