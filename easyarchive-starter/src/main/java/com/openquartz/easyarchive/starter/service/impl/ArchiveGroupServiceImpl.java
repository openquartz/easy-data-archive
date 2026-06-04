package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.common.enums.ArchiveTaskStatusEnum;
import com.openquartz.easyarchive.common.enums.DatasourceStatusEnum;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupItemStatsView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupTaskStatsView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveGroupOperationLogPresenter;
import com.openquartz.easyarchive.starter.service.ArchiveGroupService;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 归档分组服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveGroupServiceImpl implements ArchiveGroupService {

    private final ArchiveGroupMapper groupMapper;
    private final ArchiveConnectionMapper archiveConnectionMapper;
    private final ArchiveGroupExecuteTaskMapper taskMapper;
    private final ArchiveGroupItemByIdMapper idItemMapper;
    private final ArchiveGroupItemByTimeMapper timeItemMapper;
    private final DataPermissionService dataPermissionService;
    private final ArchiveGroupOperationLogPresenter archiveGroupOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<ArchiveGroupView> findAll(Integer enableStatus) {
        List<ArchiveGroup> groups;
        if (dataPermissionService.isAdmin()) {
            groups = groupMapper.selectList(enableStatus);
        } else {
            Long userId = dataPermissionService.getCurrentUser().getUserId();
            groups = groupMapper.selectAuthorizedList(userId, enableStatus);
        }
        List<ArchiveGroupView> result = new ArrayList<>(groups.size());
        if (groups.isEmpty()) {
            return result;
        }
        List<Long> groupIds = new ArrayList<>(groups.size());
        for (ArchiveGroup group : groups) {
            groupIds.add(group.getId());
        }
        Map<Long, ArchiveGroupExecuteTask> activeTaskByGroupId = new HashMap<>();
        List<ArchiveGroupExecuteTask> activeTasks = taskMapper.selectLatestActiveByGroupIds(groupIds);
        if (activeTasks != null) {
            for (ArchiveGroupExecuteTask activeTask : activeTasks) {
                activeTaskByGroupId.put(activeTask.getGroupId(), activeTask);
            }
        }
        for (ArchiveGroup group : groups) {
            result.add(toView(group, activeTaskByGroupId.get(group.getId())));
        }
        return result;
    }

    @Override
    public List<ArchiveGroup> tree() {
        if (dataPermissionService.isAdmin()) {
            return groupMapper.selectList(null);
        }
        Long userId = dataPermissionService.getCurrentUser().getUserId();
        return groupMapper.selectAuthorizedList(userId, null);
    }

    @Override
    public ArchiveGroupView findById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        ArchiveGroup group;
        if (dataPermissionService.isAdmin()) {
            group = groupMapper.selectById(id);
        } else {
            Long userId = dataPermissionService.getCurrentUser().getUserId();
            group = groupMapper.selectAuthorizedById(userId, id);
        }
        if (group == null) {
            return null;
        }
        ArchiveGroupExecuteTask activeTask = taskMapper.selectLatestActiveByGroupId(id);
        return toView(group, activeTask);
    }

    @Override
    public ArchiveGroupOverviewView findOverview(Long id) {
        dataPermissionService.assertGroupReadable(id);
        ArchiveGroup group = ensureExists(id);

        int idTypeCount = idItemMapper.countByGroupId(id);
        int timeTypeCount = timeItemMapper.countByGroupId(id);
        int enabledCount = idItemMapper.countByGroupIdAndStatus(id, EnableStatusEnum.ENABLED.getCode())
                + timeItemMapper.countByGroupIdAndStatus(id, EnableStatusEnum.ENABLED.getCode());

        ArchiveGroupExecuteTask latestTask = taskMapper.selectLatestByGroupId(id);
        List<ArchiveGroupExecuteTask> recentTasks = taskMapper.selectRecentByGroupId(id, 10);

        ArchiveGroupItemStatsView itemStats = new ArchiveGroupItemStatsView();
        long totalItemCount = (long) idTypeCount + timeTypeCount;
        itemStats.setTotalCount(totalItemCount);
        itemStats.setEnabledCount((long) enabledCount);
        itemStats.setDisabledCount(totalItemCount - enabledCount);
        itemStats.setIdTypeCount((long) idTypeCount);
        itemStats.setTimeTypeCount((long) timeTypeCount);

        ArchiveGroupTaskStatsView taskStats = new ArchiveGroupTaskStatsView();
        taskStats.setTotalCount((long) taskMapper.countByGroupId(id));
        taskStats.setSuccessCount((long) taskMapper.countByGroupIdAndStatus(id, ArchiveTaskStatusEnum.SUCCESS.getCode()));
        taskStats.setFailedCount((long) taskMapper.countByGroupIdAndStatus(id, ArchiveTaskStatusEnum.FAILED.getCode()));
        taskStats.setRunningCount((long) taskMapper.countActiveByGroupId(id));
        if (latestTask != null) {
            taskStats.setLastExecuteStatus(latestTask.getExecuteStatus());
            taskStats.setLastExecuteTime(latestTask.getStartTime() == null ? null : latestTask.getStartTime().getTime());
        }

        ArchiveGroupOverviewView overview = new ArchiveGroupOverviewView();
        overview.setGroup(toView(group, isActiveTask(latestTask) ? latestTask : null));
        overview.setItemStats(itemStats);
        overview.setTaskStats(taskStats);
        overview.setRecentTasks(recentTasks == null ? Collections.emptyList() : recentTasks);
        return overview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveGroup create(ArchiveGroup group) {
        dataPermissionService.assertAdmin();
        validateForSave(group, true);
        if (group.getEnableStatus() == null) {
            group.setEnableStatus(EnableStatusEnum.ENABLED.getCode());
        } else {
            validateEnableStatus(group.getEnableStatus());
        }
        groupMapper.insert(group);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildCreate(group));
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveGroup update(ArchiveGroup group) {
        dataPermissionService.assertAdmin();
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        ArchiveGroup before = ensureExists(group.getId());
        ensureNoActiveTask(group.getId(), "分组存在执行中的任务，无法编辑");
        validateForSave(group, false);
        if (group.getEnableStatus() != null) {
            validateEnableStatus(group.getEnableStatus());
        }
        groupMapper.update(group);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildUpdate(before, group));
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer enableStatus) {
        dataPermissionService.assertAdmin();
        ArchiveGroup before = ensureExists(id);
        validateEnableStatus(enableStatus);
        ensureNoActiveTask(id, "分组存在执行中的任务，无法修改状态");
        groupMapper.updateStatus(id, enableStatus);
        ArchiveGroup after = groupMapper.selectById(id);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildStatusUpdate(before, after));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        dataPermissionService.assertAdmin();
        ArchiveGroup before = ensureExists(id);
        ensureNoActiveTask(id, "分组存在执行中的任务，无法删除");
        groupMapper.deleteById(id);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildDelete(before));
    }

    private ArchiveGroupView toView(ArchiveGroup group) {
        ArchiveGroupExecuteTask activeTask = taskMapper.selectLatestActiveByGroupId(group.getId());
        return toView(group, activeTask);
    }

    private ArchiveGroupView toView(ArchiveGroup group, ArchiveGroupExecuteTask activeTask) {
        ArchiveGroupView view = new ArchiveGroupView();
        BeanUtils.copyProperties(group, view);
        boolean hasActiveTask = activeTask != null;
        if (hasActiveTask) {
            view.setActiveTaskId(activeTask.getId());
            view.setActiveTaskStatus(activeTask.getExecuteStatus());
            view.setActiveTaskStartTime(activeTask.getStartTime());
        }
        boolean canTrigger = !hasActiveTask && EnableStatusEnum.isEnabled(group.getEnableStatus());
        view.setCanTrigger(canTrigger);
        view.setCanCancelActiveTask(hasActiveTask);
        view.setCanViewActiveTask(hasActiveTask);
        return view;
    }

    private boolean isActiveTask(ArchiveGroupExecuteTask task) {
        return task != null && ArchiveTaskStatusEnum.isActive(task.getExecuteStatus());
    }

    private ArchiveGroup ensureExists(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        ArchiveGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new IllegalArgumentException("归档分组不存在");
        }
        return group;
    }

    private void ensureNoActiveTask(Long id, String message) {
        if (taskMapper.countActiveByGroupId(id) > 0) {
            throw new IllegalStateException(message);
        }
    }

    private void validateForSave(ArchiveGroup group, boolean create) {
        if (group == null) {
            throw new IllegalArgumentException("归档分组不能为空");
        }
        if (group.getGroupCode() != null) {
            group.setGroupCode(group.getGroupCode().trim());
        }
        if (group.getGroupName() != null) {
            group.setGroupName(group.getGroupName().trim());
        }
        if (group.getGroupCode() == null || group.getGroupCode().isEmpty()) {
            throw new IllegalArgumentException("分组编码不能为空");
        }
        if (group.getGroupName() == null || group.getGroupName().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        if (group.getSourceDatasourceId() == null || group.getTargetDatasourceId() == null) {
            throw new IllegalArgumentException("源和目标数据源不能为空");
        }
        validateDatasourceEnabled(group.getSourceDatasourceId(), "源归档连接必须为已启用状态");
        validateDatasourceEnabled(group.getTargetDatasourceId(), "目标归档连接必须为已启用状态");
        ArchiveGroup existing = groupMapper.selectByCode(group.getGroupCode());
        if (existing != null && (create || !existing.getId().equals(group.getId()))) {
            throw new IllegalArgumentException("分组编码已存在");
        }
    }

    private void validateDatasourceEnabled(Long datasourceId, String message) {
        ArchiveConnection datasource = archiveConnectionMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new IllegalArgumentException("归档连接不存在");
        }
        if (!DatasourceStatusEnum.isEnabled(datasource.getStatus())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateEnableStatus(Integer enableStatus) {
        if (EnableStatusEnum.fromCode(enableStatus) == null) {
            throw new IllegalArgumentException("启用状态不合法");
        }
    }
}
