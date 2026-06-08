package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.security.CurrentUserInfo;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final SysUserMapper sysUserMapper;

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
        currentUser.setRoleCode(RoleConstants.normalizeRoleCode(user.getRoleCode()));
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
}
