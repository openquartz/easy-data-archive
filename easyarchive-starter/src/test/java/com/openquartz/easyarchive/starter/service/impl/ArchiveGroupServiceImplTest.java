package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.notification.inapp.ArchiveInAppNotificationService;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.model.dto.RecentTaskVO;
import com.openquartz.easyarchive.starter.operationlog.OperationLogCommand;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveGroupOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
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

    private static final int DATASOURCE_STATUS_UNTESTED = 0;
    private static final int DATASOURCE_STATUS_ENABLED = 1;
    private static final int DATASOURCE_STATUS_DISABLED = 2;

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveConnectionMapper archiveConnectionMapper = mock(ArchiveConnectionMapper.class);
    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveGroupItemByIdMapper idItemMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeItemMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final CurrentUserService currentUserService = adminCurrentUserService();
    private final ArchiveResourceAccessService archiveResourceAccessService = mock(ArchiveResourceAccessService.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final ArchiveInAppNotificationService inAppNotificationService = mock(ArchiveInAppNotificationService.class);
    private final ArchiveGroupOperationLogPresenter archiveGroupOperationLogPresenter = mock(ArchiveGroupOperationLogPresenter.class);
    private final OperationLogRecorder operationLogRecorder = mock(OperationLogRecorder.class);
    private final ArchiveGroupServiceImpl service =
            new ArchiveGroupServiceImpl(groupMapper, archiveConnectionMapper, taskMapper, idItemMapper, timeItemMapper,
                    sysUserMapper, currentUserService, archiveResourceAccessService, dataPermissionService,
                    inAppNotificationService, archiveGroupOperationLogPresenter, operationLogRecorder);

    @Test
    void shouldRejectDuplicateGroupCodeOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup existing = new ArchiveGroup();
        existing.setId(1L);
        existing.setGroupCode("ORDER_ARCHIVE");
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = new ArchiveGroup();
        input.setGroupCode("ORDER_ARCHIVE");
        input.setGroupName("Order Archive");
        input.setSourceDatasourceId(1L);
        input.setTargetDatasourceId(2L);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_CODE_DUPLICATED, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectSoftDeletedGroupCodeOnCreateBecauseCodesAreNotReusable() {
        stubEnabledDatasources();
        ArchiveGroup existing = new ArchiveGroup();
        existing.setId(1L);
        existing.setGroupCode("ORDER_ARCHIVE");
        existing.setDeleted(1L);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = enabledGroup();
        input.setId(null);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_CODE_DUPLICATED, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectBlankGroupCodeOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupCode("   ");

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_CODE_REQUIRED, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectBlankGroupNameOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupName("");

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NAME_REQUIRED, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectMissingDatasourceOnCreate() {
        ArchiveGroup missingSource = enabledGroup();
        missingSource.setId(null);
        missingSource.setSourceDatasourceId(null);

        StarterManageException sourceError = assertThrows(StarterManageException.class, () -> service.create(missingSource));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_DATASOURCE_REQUIRED, sourceError.getErrorCode());
        verify(groupMapper, never()).insert(any());

        ArchiveGroup missingTarget = enabledGroup();
        missingTarget.setId(null);
        missingTarget.setTargetDatasourceId(null);

        StarterManageException targetError = assertThrows(StarterManageException.class, () -> service.create(missingTarget));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_DATASOURCE_REQUIRED, targetError.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectEnabledNotificationWithoutChannelOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setNotifyEnabled(1);
        input.setNotifyWebhookUrl("https://open.feishu.cn/open-apis/bot/hook/test");

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOTIFY_CHANNEL_REQUIRED, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldAllowInAppNotificationWithoutWebhookOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setNotifyEnabled(1);
        input.setNotifyChannel("IN_APP");
        input.setOwnerUserId(9L);
        com.openquartz.easyarchive.core.common.SysUser owner = new com.openquartz.easyarchive.core.common.SysUser();
        owner.setStatus(0);
        when(sysUserMapper.selectById(9L)).thenReturn(owner);

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldRejectInAppNotificationWithoutOwnerOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setNotifyEnabled(1);
        input.setNotifyChannel("IN_APP");

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOTIFY_OWNER_REQUIRED, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldCreateValidGroupWithDefaultStatus() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setEnableStatus(null);

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals(0, input.getEnableStatus());
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldPopulateRootHierarchyFieldsOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupLevel(null);
        input.setGroupPath(null);

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals(1, input.getGroupLevel());
        assertEquals("/", input.getGroupPath());
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldRejectInvalidStatusOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setEnableStatus(2);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(input));
        assertEquals(StarterErrorCode.ENABLE_STATUS_INVALID, error.getErrorCode());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldTrimGroupCodeAndNameBeforeCreate() {
        stubEnabledDatasources();
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
        stubEnabledDatasources();
        ArchiveGroup existing = enabledGroup();
        existing.setId(99L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = enabledGroup();

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.update(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_CODE_DUPLICATED, error.getErrorCode());
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldRejectUpdateWhenGroupDoesNotExist() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        when(groupMapper.selectById(10L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.update(input));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND, error.getErrorCode());
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldRejectInvalidStatusOnUpdate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setEnableStatus(-1);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.update(input));
        assertEquals(StarterErrorCode.ENABLE_STATUS_INVALID, error.getErrorCode());
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldAllowNullStatusOnUpdateForPartialUpdate() {
        stubEnabledDatasources();
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
        stubEnabledDatasources();
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
        stubEnabledDatasources();
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
    void shouldAllowValidWebhookNotificationConfigOnCreate() {
        stubEnabledDatasources();
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setNotifyEnabled(1);
        input.setNotifyChannel("FEISHU");
        input.setNotifyWebhookUrl(" https://open.feishu.cn/open-apis/bot/hook/test ");

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals("https://open.feishu.cn/open-apis/bot/hook/test", input.getNotifyWebhookUrl());
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldUpdateExistingGroupWhenCodeBelongsToSameGroup() {
        stubEnabledDatasources();
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

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.updateStatus(10L, 2));
        assertEquals(StarterErrorCode.ENABLE_STATUS_INVALID, error.getErrorCode());
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    @Test
    void shouldRejectStatusUpdateWhenGroupDoesNotExist() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.updateStatus(10L, 0));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND, error.getErrorCode());
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    @Test
    void shouldRejectCreateWhenSourceDatasourceIsNotEnabled() {
        when(archiveConnectionMapper.selectById(1L)).thenReturn(datasourceWithStatus(1L, DATASOURCE_STATUS_UNTESTED));
        when(archiveConnectionMapper.selectById(2L)).thenReturn(datasourceWithStatus(2L, DATASOURCE_STATUS_ENABLED));

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.create(enabledGroup()));
        assertEquals(StarterErrorCode.DATASOURCE_ENABLE_STATUS_REQUIRED, error.getErrorCode());
        assertEquals("源归档连接必须为已启用状态", error.getMessage());
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectUpdateWhenTargetDatasourceIsDisabled() {
        when(archiveConnectionMapper.selectById(1L)).thenReturn(datasourceWithStatus(1L, DATASOURCE_STATUS_ENABLED));
        when(archiveConnectionMapper.selectById(2L)).thenReturn(datasourceWithStatus(2L, DATASOURCE_STATUS_DISABLED));
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.update(enabledGroup()));
        assertEquals(StarterErrorCode.DATASOURCE_ENABLE_STATUS_REQUIRED, error.getErrorCode());
        assertEquals("目标归档连接必须为已启用状态", error.getMessage());
        verify(groupMapper, never()).update(any());
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

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.delete(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND, error.getErrorCode());
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

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.findOverview(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND, error.getErrorCode());
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

        assertEquals(1, overview.getRecentTasks().size());
        assertEquals(99L, overview.getRecentTasks().get(0).getId());
        assertEquals(ArchiveGroupExecuteTask.STATUS_RUNNING, overview.getRecentTasks().get(0).getExecuteStatus());
        verify(taskMapper, never()).selectLatestActiveByGroupId(10L);
    }

    @Test
    void shouldExposeActiveTaskRuntimeSnapshotInOverviewGroup() {
        ArchiveGroup group = enabledGroup();
        ArchiveGroupExecuteTask activeTask = new ArchiveGroupExecuteTask();
        activeTask.setId(101L);
        activeTask.setGroupId(10L);
        activeTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        activeTask.setStartTime(new Date(1704067200000L));
        activeTask.setProcessedRecords(1234L);
        activeTask.setProcessedSpeed(new BigDecimal("56.78"));
        Date heartbeatTime = new Date(1704067260000L);
        activeTask.setHeartbeatTime(heartbeatTime);

        when(groupMapper.selectById(10L)).thenReturn(group);
        when(idItemMapper.countByGroupId(10L)).thenReturn(0);
        when(idItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(0);
        when(timeItemMapper.countByGroupId(10L)).thenReturn(0);
        when(timeItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(0);
        when(taskMapper.countByGroupId(10L)).thenReturn(1);
        when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_SUCCESS)).thenReturn(0);
        when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_FAILED)).thenReturn(0);
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);
        when(taskMapper.selectLatestByGroupId(10L)).thenReturn(activeTask);
        when(taskMapper.selectRecentByGroupId(10L, 10)).thenReturn(Arrays.asList(activeTask));

        ArchiveGroupOverviewView overview = service.findOverview(10L);

        assertEquals(1234L, overview.getGroup().getActiveTaskProcessedRecords());
        assertEquals(new BigDecimal("56.78"), overview.getGroup().getActiveTaskProcessedSpeed());
        assertEquals(heartbeatTime, overview.getGroup().getActiveTaskHeartbeatTime());
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
    void shouldExposeActiveTaskRuntimeSnapshotInGroupView() {
        ArchiveGroup group = enabledGroup();
        ArchiveGroupExecuteTask activeTask = new ArchiveGroupExecuteTask();
        activeTask.setId(88L);
        activeTask.setGroupId(10L);
        activeTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        activeTask.setProcessedRecords(1234L);
        activeTask.setProcessedSpeed(new BigDecimal("56.78"));
        Date heartbeatTime = new Date(1704067200000L);
        activeTask.setHeartbeatTime(heartbeatTime);
        when(groupMapper.selectList(null)).thenReturn(Arrays.asList(group));
        when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Arrays.asList(activeTask));

        List<ArchiveGroupView> result = service.findAll(null);

        assertEquals(1, result.size());
        assertEquals(1234L, result.get(0).getActiveTaskProcessedRecords());
        assertEquals(new BigDecimal("56.78"), result.get(0).getActiveTaskProcessedSpeed());
        assertEquals(heartbeatTime, result.get(0).getActiveTaskHeartbeatTime());
    }

    @Test
    void shouldRejectDeleteWhenGroupHasActiveTask() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.delete(10L));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_ACTIVE_TASK_CONFLICT, error.getErrorCode());
        verify(groupMapper, never()).deleteById(any());
    }

    @Test
    void shouldRejectStatusUpdateWhenGroupHasActiveTask() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);

        StarterManageException error = assertThrows(StarterManageException.class, () -> service.updateStatus(10L, 1));
        assertEquals(StarterErrorCode.ARCHIVE_GROUP_ACTIVE_TASK_CONFLICT, error.getErrorCode());
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

    private void stubEnabledDatasources() {
        when(archiveConnectionMapper.selectById(1L)).thenReturn(datasourceWithStatus(1L, DATASOURCE_STATUS_ENABLED));
        when(archiveConnectionMapper.selectById(2L)).thenReturn(datasourceWithStatus(2L, DATASOURCE_STATUS_ENABLED));
    }

    private ArchiveConnection datasourceWithStatus(Long id, int status) {
        ArchiveConnection datasource = new ArchiveConnection();
        datasource.setId(id);
        datasource.setStatus(status);
        return datasource;
    }

    private static CurrentUserService adminCurrentUserService() {
        CurrentUserInfo adminInfo = new CurrentUserInfo();
        adminInfo.setUserId(1L);
        adminInfo.setUsername("admin");
        adminInfo.setRoleCode("platform_admin");
        CurrentUserService service = mock(CurrentUserService.class);
        when(service.getCurrentUser()).thenReturn(adminInfo);
        when(service.isAdmin()).thenReturn(true);
        return service;
    }
}
