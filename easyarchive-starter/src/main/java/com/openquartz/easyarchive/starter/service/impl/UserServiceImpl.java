package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.UserOperationLogPresenter;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
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
    private final DataPermissionService dataPermissionService;
    private final UserOperationLogPresenter userOperationLogPresenter;
    private final OperationLogRecorder operationLogRecorder;

    @Override
    public List<SysUser> findAll() {
        dataPermissionService.assertAdmin();
        return normalizeUsers(userMapper.selectList(null));
    }

    @Override
    public SysUser findById(Long id) {
        dataPermissionService.assertAdmin();
        return normalizeUser(userMapper.selectById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser create(SysUser user) {
        dataPermissionService.assertAdmin();
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
        dataPermissionService.assertAdmin();
        SysUser before = userMapper.selectById(user.getId());
        if (before == null) {
            throw new StarterManageException(StarterErrorCode.USER_NOT_FOUND);
        }
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
        dataPermissionService.assertAdmin();
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
