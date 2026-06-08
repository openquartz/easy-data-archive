package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.mapper.UserDatasourcePermissionMapper;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Deprecated
@Service
@RequiredArgsConstructor
public class DataPermissionServiceImpl implements DataPermissionService {

    private final SysUserMapper sysUserMapper;
    private final UserDatasourcePermissionMapper permissionMapper;
    private final ArchiveGroupMapper groupMapper;
    private final ArchiveGroupExecuteTaskMapper taskMapper;

    @Override
    public CurrentUserInfo getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new StarterManageException(StarterErrorCode.CURRENT_USER_MISSING);
        }

        String username;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) {
            username = ((User) principal).getUsername();
        } else {
            username = String.valueOf(principal);
        }

        SysUser user = sysUserMapper.selectByUsername(username);
        if (user == null) {
            throw new StarterManageException(StarterErrorCode.CURRENT_USER_NOT_FOUND);
        }

        CurrentUserInfo currentUser = new CurrentUserInfo();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setRoleCode(com.openquartz.easyarchive.starter.security.RoleConstants.normalizeRoleCode(user.getRoleCode()));
        return currentUser;
    }

    @Override
    public boolean isAdmin() {
        return getCurrentUser().isAdmin();
    }

    @Override
    public void assertAdmin() {
        if (!isAdmin()) {
            throw new StarterManageException(StarterErrorCode.ADMIN_PERMISSION_REQUIRED);
        }
    }

    @Override
    public Set<Long> getAuthorizedDatasourceIds(Long userId) {
        List<Long> datasourceIds = permissionMapper.selectDatasourceIdsByUserId(userId, "READ");
        return new HashSet<>(datasourceIds);
    }

    @Override
    public void assertDatasourceReadable(Long datasourceId) {
        if (datasourceId == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_ID_REQUIRED);
        }
        if (isAdmin()) {
            return;
        }
        CurrentUserInfo currentUser = getCurrentUser();
        if (!getAuthorizedDatasourceIds(currentUser.getUserId()).contains(datasourceId)) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_ACCESS_DENIED);
        }
    }

    @Override
    public void assertGroupReadable(Long groupId) {
        if (groupId == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        if (isAdmin()) {
            return;
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
        }
        assertDatasourceReadable(group.getSourceDatasourceId());
    }

    @Override
    public void assertTaskReadable(Long taskId) {
        if (taskId == null) {
            throw new StarterManageException(StarterErrorCode.TASK_ID_REQUIRED);
        }
        if (isAdmin()) {
            return;
        }
        ArchiveGroupExecuteTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new StarterManageException(StarterErrorCode.TASK_NOT_FOUND);
        }
        assertGroupReadable(task.getGroupId());
    }
}
