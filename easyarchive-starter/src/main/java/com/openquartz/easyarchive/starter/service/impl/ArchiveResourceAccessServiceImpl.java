package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.security.model.DatasourcePermissionLevelEnum;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.DatasourceAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArchiveResourceAccessServiceImpl implements ArchiveResourceAccessService {

    private final CurrentUserService currentUserService;
    private final DatasourceAuthorizationService datasourceAuthorizationService;
    private final ArchiveGroupMapper groupMapper;
    private final ArchiveGroupExecuteTaskMapper taskMapper;

    @Override
    public void assertGroupAccessible(Long groupId) {
        if (groupId == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            return;
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
        }
        Long userId = currentUser.getUserId();
        datasourceAuthorizationService.assertPermission(userId, group.getSourceDatasourceId(), DatasourcePermissionLevelEnum.USE);
        datasourceAuthorizationService.assertPermission(userId, group.getTargetDatasourceId(), DatasourcePermissionLevelEnum.USE);
    }

    @Override
    public void assertGroupManageable(Long groupId) {
        if (groupId == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            return;
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
        }
        Long userId = currentUser.getUserId();
        if (RoleConstants.isNormalUser(currentUser.getRoleCode()) && userId.equals(group.getOwnerUserId())) {
            return;
        }
        datasourceAuthorizationService.assertPermission(userId, group.getSourceDatasourceId(), DatasourcePermissionLevelEnum.MANAGE);
        datasourceAuthorizationService.assertPermission(userId, group.getTargetDatasourceId(), DatasourcePermissionLevelEnum.MANAGE);
    }

    @Override
    public void assertTaskAccessible(Long taskId) {
        if (taskId == null) {
            throw new StarterManageException(StarterErrorCode.TASK_ID_REQUIRED);
        }
        CurrentUserInfo currentUser = currentUserService.getCurrentUser();
        if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
            return;
        }
        ArchiveGroupExecuteTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new StarterManageException(StarterErrorCode.TASK_NOT_FOUND);
        }
        assertGroupAccessible(task.getGroupId());
    }
}
