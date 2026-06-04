package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveTaskOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveTaskLogServiceImplTest {

    private final ArchiveLogRepository archiveLogRepository = mock(ArchiveLogRepository.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final ArchiveTaskOperationLogPresenter presenter = mock(ArchiveTaskOperationLogPresenter.class);
    private final OperationLogRecorder recorder = mock(OperationLogRecorder.class);
    private final ArchiveTaskLogServiceImpl service = new ArchiveTaskLogServiceImpl(
            archiveLogRepository, taskMapper, dataPermissionService, presenter, recorder);

    @Test
    void shouldUseAuthorizedTaskPageForNormalUser() {
        when(dataPermissionService.isAdmin()).thenReturn(false);
        when(dataPermissionService.getCurrentUser()).thenReturn(currentUser());
        when(taskMapper.selectPageByUser(2L, 0, 20, "1")).thenReturn(Collections.emptyList());
        when(taskMapper.countByUser(2L, "1")).thenReturn(0);

        Map<String, Object> result = service.queryTasks(1, 20, "1");

        assertEquals(0, result.get("total"));
        verify(taskMapper).selectPageByUser(2L, 0, 20, "1");
    }

    @Test
    void shouldCheckTaskPermissionBeforeCancel() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(11L);
        task.setGroupId(21L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        when(archiveLogRepository.queryTaskById(11L)).thenReturn(task);
        when(presenter.buildCancel(task, "stop", "取消中")).thenReturn(command("CANCEL"));
        doNothing().when(dataPermissionService).assertTaskReadable(11L);

        service.cancelTask(11L, "stop");

        verify(dataPermissionService).assertTaskReadable(11L);
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
        doNothing().when(dataPermissionService).assertTaskReadable(12L);

        service.cancelTask(12L, "manual");

        verify(archiveLogRepository).updateTaskStatus(12L, ArchiveGroupExecuteTask.STATUS_CANCELLED);
        verify(recorder).record(any());
    }

    @Test
    void shouldRecordDuplicateCancelResultWithoutUpdatingTask() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(13L);
        task.setGroupId(23L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLING);
        when(archiveLogRepository.queryTaskById(13L)).thenReturn(task);
        when(presenter.buildCancel(task, "manual", "重复请求，无需处理")).thenReturn(command("CANCEL"));
        doNothing().when(dataPermissionService).assertTaskReadable(13L);

        service.cancelTask(13L, "manual");

        verify(archiveLogRepository, never()).updateTaskStatus(13L, ArchiveGroupExecuteTask.STATUS_CANCELLING);
        verify(recorder).record(any());
    }

    @Test
    void shouldRecordFailureBeforeThrowingForTerminalTask() {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(14L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLED);
        when(archiveLogRepository.queryTaskById(14L)).thenReturn(task);
        doNothing().when(dataPermissionService).assertTaskReadable(14L);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.cancelTask(14L, "manual"));
        assertEquals(StarterErrorCode.ARCHIVE_TASK_TERMINAL_CANNOT_CANCEL, error.getErrorCode());

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
        currentUserInfo.setRoleCode("USER");
        return currentUserInfo;
    }

    private static OperationLogCommand command(String action) {
        return new OperationLogCommand("ARCHIVE_TASK", action, action, "ARCHIVE_TASK", 1L, "TASK_1", action, null);
    }
}
