package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.dto.LoginRequest;
import com.openquartz.easyarchive.starter.model.dto.LoginResponse;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import com.openquartz.easyarchive.starter.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        SysUser user = sysUserMapper.selectByUsername(loginRequest.getUsername());
        if (user == null || user.getDeleted() != null && user.getDeleted() != 0) {
            throw new BadCredentialsException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() != 0) {
            throw new BadCredentialsException("用户已禁用");
        }
        if (!passwordMatches(loginRequest.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("用户名或密码错误");
        }

        String roleCode = RoleConstants.normalizeRoleCode(user.getRoleCode());
        User principal = new User(user.getUsername(), user.getPassword(), Collections.emptyList());
        String token = jwtTokenUtil.generateToken(principal);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setRoleCode(roleCode);
        response.setPermissions(Collections.singletonList(roleCode));
        response.setExpiresIn(86400L);
        return response;
    }

    private boolean passwordMatches(String rawPassword, String encodedPassword) {
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
    }

    @Override
    public Object getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
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
            return null;
        }
        Map<String, Object> view = new HashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("realName", user.getRealName());
        view.put("status", user.getStatus());
        view.put("roleCode", RoleConstants.normalizeRoleCode(user.getRoleCode()));
        view.put("isAdmin", RoleConstants.isAdmin(user.getRoleCode()));
        return view;
    }
}
