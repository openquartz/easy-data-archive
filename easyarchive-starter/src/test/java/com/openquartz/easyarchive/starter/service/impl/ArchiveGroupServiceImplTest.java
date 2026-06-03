package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveGroupOperationLogPresenter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveGroupServiceImplTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveGroupItemByIdMapper idItemMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeItemMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final DataPermissionService dataPermissionService = adminPermissionService();
    private final ArchiveGroupOperationLogPresenter archiveGroupOperationLogPresenter = mock(ArchiveGroupOperationLogPresenter.class);
    private final OperationLogRecorder operationLogRecorder = mock(OperationLogRecorder.class);
    private final ArchiveGroupServiceImpl service =
            new ArchiveGroupServiceImpl(groupMapper, taskMapper, idItemMapper, timeItemMapper, dataPermissionService,
                    archiveGroupOperationLogPresenter, operationLogRecorder);

    @Test
    void shouldRejectDuplicateGroupCodeOnCreate() {
        ArchiveGroup existing = new ArchiveGroup();
        existing.setId(1L);
        existing.setGroupCode("ORDER_ARCHIVE");
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = new ArchiveGroup();
        input.setGroupCode("ORDER_ARCHIVE");
        input.setGroupName("Order Archive");
        input.setSourceDatasourceId(1L);
        input.setTargetDatasourceId(2L);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectSoftDeletedGroupCodeOnCreateBecauseCodesAreNotReusable() {
        ArchiveGroup existing = new ArchiveGroup();
        existing.setId(1L);
        existing.setGroupCode("ORDER_ARCHIVE");
        existing.setDeleted(1L);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = enabledGroup();
        input.setId(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectBlankGroupCodeOnCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupCode("   ");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectBlankGroupNameOnCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupName("");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectMissingDatasourceOnCreate() {
        ArchiveGroup missingSource = enabledGroup();
        missingSource.setId(null);
        missingSource.setSourceDatasourceId(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(missingSource));
        verify(groupMapper, never()).insert(any());

        ArchiveGroup missingTarget = enabledGroup();
        missingTarget.setId(null);
        missingTarget.setTargetDatasourceId(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(missingTarget));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldCreateValidGroupWithDefaultStatus() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setEnableStatus(null);

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals(0, input.getEnableStatus());
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldRejectInvalidStatusOnCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setEnableStatus(2);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldTrimGroupCodeAndNameBeforeCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupCode(" ORDER_ARCHIVE ");
        input.setGroupName(" Order Archive ");

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals("ORDER_ARCHIVE", input.getGroupCode());
        assertEquals("Order Archive", input.getGroupName());
        verify(groupMapper).selectByCode("ORDER_ARCHIVE");
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldRejectDuplicateGroupCodeOnUpdate() {
        ArchiveGroup existing = enabledGroup();
        existing.setId(99L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = enabledGroup();

        assertThrows(IllegalArgumentException.class, () -> service.update(input));
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldRejectUpdateWhenGroupDoesNotExist() {
        ArchiveGroup input = enabledGroup();
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.update(input));
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldRejectInvalidStatusOnUpdate() {
        ArchiveGroup input = enabledGroup();
        input.setEnableStatus(-1);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        assertThrows(IllegalArgumentException.class, () -> service.update(input));
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldAllowNullStatusOnUpdateForPartialUpdate() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setEnableStatus(null);
        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        verify(groupMapper).update(input);
    }

    @Test
    void shouldTrimGroupCodeAndNameBeforeUpdate() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setGroupCode(" ORDER_ARCHIVE ");
        input.setGroupName(" Order Archive ");
        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        assertEquals("ORDER_ARCHIVE", input.getGroupCode());
        assertEquals("Order Archive", input.getGroupName());
        verify(groupMapper).selectByCode("ORDER_ARCHIVE");
        verify(groupMapper).update(input);
    }

    @Test
    void shouldUpdateGroupCodeWhenNewCodeIsNotDuplicate() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setGroupCode(" ORDER_ARCHIVE_NEW ");
        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE_NEW")).thenReturn(null);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        assertEquals("ORDER_ARCHIVE_NEW", input.getGroupCode());
        verify(groupMapper).selectByCode("ORDER_ARCHIVE_NEW");
        verify(groupMapper).update(input);
    }

    @Test
    void shouldUpdateExistingGroupWhenCodeBelongsToSameGroup() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setGroupName("Order Archive Updated");

        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        verify(groupMapper).update(input);
    }

    @Test
    void shouldRejectInvalidStatusUpdate() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 2));
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    @Test
    void shouldRejectStatusUpdateWhenGroupDoesNotExist() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 0));
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    @Test
    void shouldDeleteExistingGroup() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(archiveGroupOperationLogPresenter.buildDelete(any()))
                .thenReturn(new OperationLogCommand("ARCHIVE_GROUP", "DELETE", "删除分组", "ARCHIVE_GROUP",
                        10L, "ORDER_ARCHIVE", "content", null));

        service.delete(10L);

        verify(groupMapper).deleteById(10L);
        verify(operationLogRecorder).record(any());
    }

    @Test
    void shouldRejectDeleteWhenGroupDoesNotExist() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.delete(10L));
        verify(groupMapper, never()).deleteById(any());
    }

    @Test
    void shouldReturnFlatListForTree() {
        List<ArchiveGroup> groups = Arrays.asList(enabledGroup());
        when(groupMapper.selectList(null)).thenReturn(groups);

        assertSame(groups, service.tree());
    }

    @Test
    void shouldRejectOverviewWhenGroupDoesNotExist() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.findOverview(10L));
    }

    @Test
    void shouldReturnNullWhenFindByIdMissesGroup() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertNull(service.findById(10L));
        verify(taskMapper, never()).selectLatestActiveByGroupId(any());
    }

    @Test
    void shouldAggregateOverviewStats() {
        ArchiveGroup group = enabledGroup();
        ArchiveGroupExecuteTask latestTask = new ArchiveGroupExecuteTask();
        latestTask.setId(101L);
        latestTask.setGroupId(10L);
        latestTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_FAILED);
        latestTask.setStartTime(new Date(1704067200000L));

        when(groupMapper.selectById(10L)).thenReturn(group);
        when(idItemMapper.countByGroupId(10L)).thenReturn(3);
        when(idItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(2);
        when(timeItemMapper.countByGroupId(10L)).thenReturn(4);
        when(timeItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(1);
        when(taskMapper.countByGroupId(10L)).thenReturn(12);
        when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_SUCCESS)).thenReturn(8);
        when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_FAILED)).thenReturn(3);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);
        when(taskMapper.selectLatestByGroupId(10L)).thenReturn(latestTask);
        when(taskMapper.selectRecentByGroupId(10L, 10)).thenReturn(Arrays.asList(latestTask));

        ArchiveGroupOverviewView overview = service.findOverview(10L);

        assertEquals(10L, overview.getGroup().getId());
        assertEquals(7L, overview.getItemStats().getTotalCount());
        assertEquals(3L, overview.getItemStats().getEnabledCount());
        assertEquals(4L, overview.getItemStats().getDisabledCount());
        assertEquals(3L, overview.getItemStats().getIdTypeCount());
        assertEquals(4L, overview.getItemStats().getTimeTypeCount());
        assertEquals(12L, overview.getTaskStats().getTotalCount());
        assertEquals(8L, overview.getTaskStats().getSuccessCount());
        assertEquals(3L, overview.getTaskStats().getFailedCount());
        assertEquals(1L, overview.getTaskStats().getRunningCount());
        assertEquals(ArchiveGroupExecuteTask.STATUS_FAILED, overview.getTaskStats().getLastExecuteStatus());
        assertEquals(1704067200000L, overview.getTaskStats().getLastExecuteTime());
        verify(taskMapper, never()).selectLatestActiveByGroupId(10L);
    }

    @Test
    void shouldExposeRecentTasksInOverview() {
        ArchiveGroup group = enabledGroup();
        ArchiveGroupExecuteTask latestTask = new ArchiveGroupExecuteTask();
        latestTask.setId(101L);
        latestTask.setGroupId(10L);
        latestTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_SUCCESS);
        latestTask.setStartTime(new Date(1704067200000L));

        ArchiveGroupExecuteTask recentTask = new ArchiveGroupExecuteTask();
        recentTask.setId(99L);
        recentTask.setGroupId(10L);
        recentTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        List<ArchiveGroupExecuteTask> recentTasks = Arrays.asList(recentTask);

        when(groupMapper.selectById(10L)).thenReturn(group);
        when(idItemMapper.countByGroupId(10L)).thenReturn(0);
        when(idItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(0);
        when(timeItemMapper.countByGroupId(10L)).thenReturn(0);
        when(timeItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(0);
        when(taskMapper.countByGroupId(10L)).thenReturn(1);
        when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_SUCCESS)).thenReturn(1);
        when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_FAILED)).thenReturn(0);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(0);
        when(taskMapper.selectLatestByGroupId(10L)).thenReturn(latestTask);
        when(taskMapper.selectRecentByGroupId(10L, 10)).thenReturn(recentTasks);

        ArchiveGroupOverviewView overview = service.findOverview(10L);

        assertSame(recentTasks, overview.getRecentTasks());
        assertEquals(99L, overview.getRecentTasks().get(0).getId());
        assertEquals(ArchiveGroupExecuteTask.STATUS_RUNNING, overview.getRecentTasks().get(0).getExecuteStatus());
        verify(taskMapper, never()).selectLatestActiveByGroupId(10L);
    }

    @Test
    void shouldExposeActiveTaskStateInGroupViewWithBatchLookup() {
        ArchiveGroup group = enabledGroup();
        ArchiveGroup secondGroup = enabledGroup();
        secondGroup.setId(11L);
        secondGroup.setGroupCode("ORDER_ARCHIVE_2");
        secondGroup.setGroupName("Order Archive 2");
        ArchiveGroupExecuteTask activeTask = new ArchiveGroupExecuteTask();
        activeTask.setId(88L);
        activeTask.setGroupId(10L);
        activeTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        when(groupMapper.selectList(null)).thenReturn(Arrays.asList(group, secondGroup));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Arrays.asList(activeTask));

        List<ArchiveGroupView> result = service.findAll(null);

        assertEquals(2, result.size());
        assertEquals(88L, result.get(0).getActiveTaskId());
        assertEquals(ArchiveGroupExecuteTask.STATUS_RUNNING, result.get(0).getActiveTaskStatus());
        assertEquals(Boolean.FALSE, result.get(0).getCanTrigger());
        assertEquals(Boolean.TRUE, result.get(0).getCanCancelActiveTask());
        assertEquals(Boolean.TRUE, result.get(0).getCanViewActiveTask());
        assertNull(result.get(1).getActiveTaskId());
        verify(taskMapper).selectLatestActiveByGroupIds(anyList());
        verify(taskMapper, never()).selectLatestActiveByGroupId(any());
    }

    @Test
    void shouldRejectDeleteWhenGroupHasActiveTask() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.delete(10L));
        verify(groupMapper, never()).deleteById(any());
    }

    @Test
    void shouldRejectStatusUpdateWhenGroupHasActiveTask() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);

        assertThrows(IllegalStateException.class, () -> service.updateStatus(10L, 1));
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    private static ArchiveGroup enabledGroup() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setEnableStatus(0);
        return group;
    }

    private static DataPermissionService adminPermissionService() {
        DataPermissionService service = mock(DataPermissionService.class);
        when(service.isAdmin()).thenReturn(true);
        return service;
    }
}
