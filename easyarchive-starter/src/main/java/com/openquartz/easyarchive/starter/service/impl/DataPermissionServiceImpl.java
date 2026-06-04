package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
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
            throw new IllegalStateException("未获取到当前登录用户");
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
            throw new IllegalStateException("当前登录用户不存在");
        }

        CurrentUserInfo currentUser = new CurrentUserInfo();
        currentUser.setUserId(user.getId());
        currentUser.setUsername(user.getUsername());
        currentUser.setRoleCode(user.getRoleCode());
        return currentUser;
    }

    @Override
    public boolean isAdmin() {
        return getCurrentUser().isAdmin();
    }

    @Override
    public void assertAdmin() {
        if (!isAdmin()) {
            throw new IllegalStateException("无管理员权限");
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
            throw new IllegalArgumentException("数据源ID不能为空");
        }
        if (isAdmin()) {
            return;
        }
        CurrentUserInfo currentUser = getCurrentUser();
        if (!getAuthorizedDatasourceIds(currentUser.getUserId()).contains(datasourceId)) {
            throw new IllegalStateException("无权限访问该数据源");
        }
    }

    @Override
    public void assertGroupReadable(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        if (isAdmin()) {
            return;
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("归档分组不存在");
        }
        assertDatasourceReadable(group.getSourceDatasourceId());
    }

    @Override
    public void assertTaskReadable(Long taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (isAdmin()) {
            return;
        }
        ArchiveGroupExecuteTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        assertGroupReadable(task.getGroupId());
    }
}
