package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import com.openquartz.easyarchive.starter.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;
    private final UserOperationLogPresenter userOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<SysUser> findAll() {
        currentUserService.assertAdmin();
        return normalizeUsers(userMapper.selectList(null));
    }

    @Override
    public SysUser findById(Long id) {
        currentUserService.assertAdmin();
        return normalizeUser(userMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(SysUser user) {
        CurrentUserInfo operator = currentUserService.getCurrentUser();
        assertCanCreateRole(operator, user.getRoleCode());
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        user.setRoleCode(resolveRoleCode(user.getRoleCode(), null));
        if (user.getStatus() == null) {
            user.setStatus(EnableStatusEnum.ENABLED.getCode());
        }
        userMapper.insert(user);
        operationLogRecorder.record(userOperationLogPresenter.buildCreate(user));
        return normalizeUser(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser update(SysUser user) {
        CurrentUserInfo operator = currentUserService.getCurrentUser();
        SysUser before = userMapper.selectById(user.getId());
        if (before == null) {
            throw new StarterManageException(StarterErrorCode.USER_NOT_FOUND);
        }
        assertCanUpdateUser(operator, user, before);
        boolean passwordUpdated = user.getPassword() != null && !user.getPassword().trim().isEmpty();
        if (passwordUpdated) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        } else {
            user.setPassword(null);
        }
        user.setRoleCode(resolveRoleCode(user.getRoleCode(), before.getRoleCode()));
        userMapper.update(user);
        SysUser after = userMapper.selectById(user.getId());
        if (before != null && after != null) {
            operationLogRecorder.record(userOperationLogPresenter.buildUpdate(before, after, passwordUpdated));
        }
        return normalizeUser(after != null ? after : user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        currentUserService.assertAdmin();
        SysUser before = userMapper.selectById(id);
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(status);
        userMapper.update(user);
        SysUser after = userMapper.selectById(id);
        if (before != null && after != null) {
            operationLogRecorder.record(userOperationLogPresenter.buildStatusUpdate(before, after));
        }
    }

    private void assertCanCreateRole(CurrentUserInfo operator, String targetRoleCode) {
        if (RoleConstants.isAdmin(operator.getRoleCode())) {
            return;
        }
        if (RoleConstants.isArchiveAdmin(operator.getRoleCode())
                && RoleConstants.NORMAL_USER.equals(RoleConstants.normalizeRoleCode(targetRoleCode))) {
            return;
        }
        throw new StarterManageException(StarterErrorCode.USER_ROLE_INVALID_FOR_CREATOR);
    }

    private void assertCanUpdateUser(CurrentUserInfo operator, SysUser input, SysUser existing) {
        if (RoleConstants.isAdmin(operator.getRoleCode())) {
            return;
        }
        if (RoleConstants.isArchiveAdmin(operator.getRoleCode())) {
            if (!RoleConstants.NORMAL_USER.equals(RoleConstants.normalizeRoleCode(existing.getRoleCode()))) {
                throw new StarterManageException(StarterErrorCode.ADMIN_PERMISSION_REQUIRED);
            }
            String newRole = RoleConstants.normalizeRoleCode(input.getRoleCode());
            if (!RoleConstants.NORMAL_USER.equals(newRole)) {
                throw new StarterManageException(StarterErrorCode.USER_ROLE_INVALID_FOR_CREATOR);
            }
            return;
        }
        throw new StarterManageException(StarterErrorCode.ADMIN_PERMISSION_REQUIRED);
    }

    private String resolveRoleCode(String roleCode, String fallbackRoleCode) {
        String candidate = roleCode;
        if (candidate == null || candidate.trim().isEmpty()) {
            candidate = fallbackRoleCode;
        }
        String normalized = RoleConstants.normalizeRoleCode(candidate);
        if (!RoleConstants.isSupported(normalized)) {
            throw new StarterManageException(StarterErrorCode.USER_ROLE_INVALID);
        }
        return normalized;
    }

    private List<SysUser> normalizeUsers(List<SysUser> users) {
        List<SysUser> normalizedUsers = new ArrayList<>();
        for (SysUser user : users) {
            normalizedUsers.add(normalizeUser(user));
        }
        return normalizedUsers;
    }

    private SysUser normalizeUser(SysUser user) {
        if (user == null) {
            return null;
        }
        user.setRoleCode(RoleConstants.normalizeRoleCode(user.getRoleCode()));
        return user;
    }
}
