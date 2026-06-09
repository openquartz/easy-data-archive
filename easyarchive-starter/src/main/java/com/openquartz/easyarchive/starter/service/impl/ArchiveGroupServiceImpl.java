package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.common.enums.ArchiveTaskStatusEnum;
import com.openquartz.easyarchive.common.enums.DatasourceStatusEnum;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupItemStatsView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupTaskStatsView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.model.dto.PageResult;
import com.openquartz.easyarchive.starter.model.dto.RecentTaskVO;
import com.openquartz.easyarchive.starter.model.enums.NotificationChannelEnum;
import com.openquartz.easyarchive.starter.notification.inapp.ArchiveInAppNotificationService;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveGroupOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.ArchiveGroupService;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
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
import java.util.Set;

/**
 * 归档分组服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveGroupServiceImpl implements ArchiveGroupService {

    private static final int ROOT_GROUP_LEVEL = 1;
    private static final String ROOT_GROUP_PATH = "/";

    private final ArchiveGroupMapper groupMapper;
    private final ArchiveConnectionMapper archiveConnectionMapper;
    private final ArchiveGroupExecuteTaskMapper taskMapper;
    private final ArchiveGroupItemByIdMapper idItemMapper;
    private final ArchiveGroupItemByTimeMapper timeItemMapper;
    private final SysUserMapper sysUserMapper;
    private final CurrentUserService currentUserService;
    private final ArchiveResourceAccessService archiveResourceAccessService;
    private final DataPermissionService dataPermissionService;
    private final ArchiveInAppNotificationService inAppNotificationService;
    private final ArchiveGroupOperationLogPresenter archiveGroupOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<ArchiveGroupView> findAll(Integer enableStatus) {
        List<ArchiveGroup> groups;
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            // 系统管理员 - 查看所有分组
            groups = groupMapper.selectList(enableStatus);
        } else {
            Long userId = currentUser.getUserId();
            groups = groupMapper.selectAuthorizedList(userId, enableStatus);
            if (RoleConstants.isNormalUser(currentUser.getRoleCode())) {
                // 普通用户 - 只查看自己负责的分组
                List<ArchiveGroup> filtered = new ArrayList<>(groups.size());
                for (ArchiveGroup group : groups) {
                    if (userId.equals(group.getOwnerUserId())) {
                        filtered.add(group);
                    }
                }
                groups = filtered;
            } else if (RoleConstants.isArchiveAdmin(currentUser.getRoleCode())) {
                // 归档管理员 - 查看有权限数据源的分组
                Set<Long> authorizedDatasourceIds = dataPermissionService.getAuthorizedDatasourceIds(userId);
                if (authorizedDatasourceIds != null && !authorizedDatasourceIds.isEmpty()) {
                    List<ArchiveGroup> filtered = new ArrayList<>(groups.size());
                    for (ArchiveGroup group : groups) {
                        if (authorizedDatasourceIds.contains(group.getSourceDatasourceId())) {
                            filtered.add(group);
                        }
                    }
                    groups = filtered;
                }
            }
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
    public PageResult<ArchiveGroupView> findPage(Integer enableStatus, int page, int size) {
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        int start = (page - 1) * size;

        List<ArchiveGroup> groups;
        long total;

        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            // 系统管理员 - 查看所有分组
            total = groupMapper.count(enableStatus);
            groups = groupMapper.selectPage(enableStatus, start, size);
        } else if (RoleConstants.isArchiveAdmin(currentUser.getRoleCode())) {
            // 归档管理员 - 查看有权限的数据源分组
            Long userId = currentUser.getUserId();
            total = groupMapper.countAuthorized(userId, enableStatus);
            groups = groupMapper.selectAuthorizedPage(userId, enableStatus, start, size);
        } else {
            // 普通用户 - 查看自己负责的分组
            Long userId = currentUser.getUserId();
            total = groupMapper.countByOwner(userId, enableStatus);
            groups = groupMapper.selectPageByOwner(userId, enableStatus, start, size);
        }

        List<ArchiveGroupView> result = new ArrayList<>(groups.size());
        if (groups.isEmpty()) {
            return PageResult.of(result, total, page, size);
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

        return PageResult.of(result, total, page, size);
    }

    @Override
    public List<ArchiveGroup> tree() {
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            return groupMapper.selectList(null);
        }
        Long userId = currentUser.getUserId();
        return groupMapper.selectAuthorizedList(userId, null);
    }

    @Override
    public ArchiveGroupView findById(Long id) {
        if (id == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        ArchiveGroup group;
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            group = groupMapper.selectById(id);
        } else {
            Long userId = currentUser.getUserId();
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
        archiveResourceAccessService.assertGroupAccessible(id);
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
        if (recentTasks == null || recentTasks.isEmpty()) {
            overview.setRecentTasks(Collections.emptyList());
        } else {
            List<RecentTaskVO> recentTaskVOList = new java.util.ArrayList<>(recentTasks.size());
            for (ArchiveGroupExecuteTask task : recentTasks) {
                RecentTaskVO vo = new RecentTaskVO();
                BeanUtils.copyProperties(task, vo);
                recentTaskVOList.add(vo);
            }
            overview.setRecentTasks(recentTaskVOList);
        }
        return overview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveGroup create(ArchiveGroup group) {
        validateForSave(group, true);
        validateOwnerOnCreate(group);
        populateHierarchyFields(group);
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
        if (group == null || group.getId() == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        archiveResourceAccessService.assertGroupManageable(group.getId());
        ArchiveGroup before = ensureExists(group.getId());
        ensureNoActiveTask(group.getId(), "分组存在执行中的任务，无法编辑");
        validateForSave(group, false);
        populateHierarchyFields(group);
        if (group.getEnableStatus() != null) {
            validateEnableStatus(group.getEnableStatus());
        }
        groupMapper.update(group);
        ArchiveGroup after = ensureExists(group.getId());
        inAppNotificationService.notifyOwnerChanged(before, after);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildUpdate(before, after));
        return group;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer enableStatus) {
        archiveResourceAccessService.assertGroupManageable(id);
        ArchiveGroup before = ensureExists(id);
        validateEnableStatus(enableStatus);
        ensureNoActiveTask(id, "分组存在执行中的任务，无法修改状态");
        groupMapper.updateStatus(id, enableStatus);
        ArchiveGroup after = groupMapper.selectById(id);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildStatusUpdate(before, after));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveGroup updateOwner(Long groupId, Long newOwnerUserId) {
        CurrentUserInfo operator = currentUserService.getCurrentUser();
        assertCanUpdateOwner(operator);
        validateOwnerUser(newOwnerUserId);
        archiveResourceAccessService.assertGroupManageable(groupId);
        ArchiveGroup before = ensureExists(groupId);
        ensureNoActiveTask(groupId, "分组存在执行中的任务，无法变更负责人");
        groupMapper.updateOwner(groupId, newOwnerUserId);
        ArchiveGroup after = ensureExists(groupId);
        inAppNotificationService.notifyOwnerChanged(before, after);
        operationLogRecorder.record(archiveGroupOperationLogPresenter.buildOwnerUpdate(before, after));
        return after;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        archiveResourceAccessService.assertGroupManageable(id);
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
        view.setOwnerDisplayName(resolveOwnerDisplayName(group.getOwnerUserId()));
        boolean hasActiveTask = activeTask != null;
        if (hasActiveTask) {
            view.setActiveTaskId(activeTask.getId());
            view.setActiveTaskStatus(activeTask.getExecuteStatus());
            view.setActiveTaskStartTime(activeTask.getStartTime());
            view.setActiveTaskProcessedRecords(activeTask.getProcessedRecords());
            view.setActiveTaskProcessedSpeed(activeTask.getProcessedSpeed());
            view.setActiveTaskHeartbeatTime(activeTask.getHeartbeatTime());
        }
        boolean canTrigger = !hasActiveTask && EnableStatusEnum.isEnabled(group.getEnableStatus());
        view.setCanTrigger(canTrigger);
        view.setCanCancelActiveTask(hasActiveTask);
        view.setCanViewActiveTask(hasActiveTask);
        return view;
    }

    private String resolveOwnerDisplayName(Long ownerUserId) {
        if (ownerUserId == null) {
            return null;
        }
        SysUser owner = sysUserMapper.selectById(ownerUserId);
        if (owner == null) {
            return null;
        }
        String username = owner.getUsername() == null ? "" : owner.getUsername().trim();
        String realName = owner.getRealName() == null ? "" : owner.getRealName().trim();
        if (!realName.isEmpty() && !username.isEmpty()) {
            return realName + " (" + username + ")";
        }
        if (!realName.isEmpty()) {
            return realName;
        }
        return username.isEmpty() ? null : username;
    }

    private boolean isActiveTask(ArchiveGroupExecuteTask task) {
        return task != null && ArchiveTaskStatusEnum.isActive(task.getExecuteStatus());
    }

    private ArchiveGroup ensureExists(Long id) {
        if (id == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        ArchiveGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
        }
        return group;
    }

    private void ensureNoActiveTask(Long id, String message) {
        if (taskMapper.countActiveByGroupId(id) > 0) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ACTIVE_TASK_CONFLICT, message);
        }
    }

    private void validateForSave(ArchiveGroup group, boolean create) {
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_REQUIRED);
        }
        if (group.getGroupCode() != null) {
            group.setGroupCode(group.getGroupCode().trim());
        }
        if (group.getGroupName() != null) {
            group.setGroupName(group.getGroupName().trim());
        }
        if (group.getNotifyChannel() != null) {
            group.setNotifyChannel(group.getNotifyChannel().trim().toUpperCase());
        }
        if (group.getNotifyWebhookUrl() != null) {
            group.setNotifyWebhookUrl(group.getNotifyWebhookUrl().trim());
        }
        validateOwner(group.getOwnerUserId());
        if (group.getGroupCode() == null || group.getGroupCode().isEmpty()) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_CODE_REQUIRED);
        }
        if (group.getGroupName() == null || group.getGroupName().isEmpty()) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NAME_REQUIRED);
        }
        if (group.getSourceDatasourceId() == null || group.getTargetDatasourceId() == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_DATASOURCE_REQUIRED);
        }
        validateDatasourceEnabled(group.getSourceDatasourceId(), "源归档连接必须为已启用状态");
        validateDatasourceEnabled(group.getTargetDatasourceId(), "目标归档连接必须为已启用状态");
        validateNotificationConfig(group);
        ArchiveGroup existing = groupMapper.selectByCode(group.getGroupCode());
        if (existing != null && (create || !existing.getId().equals(group.getId()))) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_CODE_DUPLICATED);
        }
    }

    private void validateDatasourceEnabled(Long datasourceId, String message) {
        ArchiveConnection datasource = archiveConnectionMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
        }
        if (!DatasourceStatusEnum.isEnabled(datasource.getStatus())) {
            throw StarterManageException.withPlaceholders(
                    StarterErrorCode.DATASOURCE_ENABLE_STATUS_REQUIRED, message.startsWith("源") ? "源" : "目标");
        }
    }

    private void validateEnableStatus(Integer enableStatus) {
        if (EnableStatusEnum.fromCode(enableStatus) == null) {
            throw new StarterManageException(StarterErrorCode.ENABLE_STATUS_INVALID);
        }
    }

    private void populateHierarchyFields(ArchiveGroup group) {
        if (group.getParentId() == null) {
            group.setGroupLevel(ROOT_GROUP_LEVEL);
            if (group.getGroupPath() == null || group.getGroupPath().trim().isEmpty()) {
                group.setGroupPath(ROOT_GROUP_PATH);
            }
            return;
        }

        ArchiveGroup parent = ensureExists(group.getParentId());
        int parentLevel = parent.getGroupLevel() == null ? ROOT_GROUP_LEVEL : parent.getGroupLevel();
        group.setGroupLevel(parentLevel + 1);

        String parentPath = parent.getGroupPath();
        if (parentPath == null || parentPath.trim().isEmpty()) {
            parentPath = ROOT_GROUP_PATH;
        }
        if (!parentPath.endsWith(ROOT_GROUP_PATH)) {
            parentPath = parentPath + ROOT_GROUP_PATH;
        }
        group.setGroupPath(parentPath + parent.getId() + ROOT_GROUP_PATH);
    }

    private void validateNotificationConfig(ArchiveGroup group) {
        Integer notifyEnabled = group.getNotifyEnabled();
        if (notifyEnabled == null) {
            group.setNotifyEnabled(0);
            notifyEnabled = 0;
        }
        if (notifyEnabled != 0 && notifyEnabled != 1) {
            throw new IllegalArgumentException("通知状态不合法");
        }
        if (notifyEnabled == 0) {
            return;
        }
        if (!NotificationChannelEnum.supports(group.getNotifyChannel())) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOTIFY_CHANNEL_REQUIRED);
        }
        NotificationChannelEnum channel = NotificationChannelEnum.valueOf(group.getNotifyChannel().trim().toUpperCase());
        if (channel == NotificationChannelEnum.IN_APP) {
            if (group.getOwnerUserId() == null) {
                throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOTIFY_OWNER_REQUIRED);
            }
            return;
        }
        if (group.getNotifyWebhookUrl() == null || group.getNotifyWebhookUrl().isEmpty()) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOTIFY_WEBHOOK_REQUIRED);
        }
    }

    private void validateOwner(Long ownerUserId) {
        if (ownerUserId == null) {
            return;
        }
        if (sysUserMapper.selectById(ownerUserId) == null) {
            throw new StarterManageException(StarterErrorCode.USER_NOT_FOUND);
        }
    }

    private void validateOwnerUser(Long ownerUserId) {
        if (ownerUserId == null) {
            return;
        }
        SysUser user = sysUserMapper.selectById(ownerUserId);
        if (user == null) {
            throw new StarterManageException(StarterErrorCode.OWNER_USER_INVALID);
        }
        if (!EnableStatusEnum.ENABLED.getCode().equals(user.getStatus())) {
            throw new StarterManageException(StarterErrorCode.OWNER_USER_DISABLED);
        }
    }

    private void validateOwnerOnCreate(ArchiveGroup group) {
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();

        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            // 系统管理员 - 可指定任意用户
            validateOwnerUser(group.getOwnerUserId());
            return;
        }

        if (RoleConstants.isArchiveAdmin(currentUser.getRoleCode())) {
            // 归档管理员 - 默认自己，可指定自己创建的普通用户
            if (group.getOwnerUserId() == null) {
                group.setOwnerUserId(currentUser.getUserId());
            } else {
                validateOwnerUser(group.getOwnerUserId());
                assertUserCreatedBy(group.getOwnerUserId(), String.valueOf(currentUser.getUserId()));
            }
            return;
        }

        // 普通用户 - 负责人只能是自己
        group.setOwnerUserId(currentUser.getUserId());
    }

    private void assertUserCreatedBy(Long targetUserId, String creatorId) {
        SysUser targetUser = sysUserMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new StarterManageException(StarterErrorCode.OWNER_USER_INVALID);
        }
        if (!creatorId.equals(targetUser.getCreatorId())) {
            throw new StarterManageException(StarterErrorCode.OWNER_USER_NOT_CREATED_BY_YOU);
        }
    }

    private void assertCanUpdateOwner(CurrentUserInfo operator) {
        if (RoleConstants.isNormalUser(operator.getRoleCode())) {
            throw new StarterManageException(StarterErrorCode.OWNER_UPDATE_NOT_ALLOWED);
        }
        // 系统管理员和归档管理员允许
    }
}
