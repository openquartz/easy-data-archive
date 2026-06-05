package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.mapper.SysUserMapper;
import com.openquartz.easyarchive.starter.model.dto.LoginRequest;
import com.openquartz.easyarchive.starter.model.dto.LoginResponse;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.security.RoleConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final SysUserMapper sysUserMapper = mock(SysUserMapper.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenUtil jwtTokenUtil = mock(JwtTokenUtil.class);

    private final AuthServiceImpl authService =
            new AuthServiceImpl(sysUserMapper, passwordEncoder, jwtTokenUtil);

    @Test
    void shouldTreatNonBcryptStoredPasswordAsBadCredentials() {
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPassword("password");
        user.setStatus(0);
        user.setDeleted(0L);

        when(sysUserMapper.selectByUsername("admin")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtTokenUtil, never()).generateToken(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldAllowEnabledUserWithStatusZeroToLogin() {
        SysUser user = new SysUser();
        user.setUsername("admin");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRealName("Admin");
        user.setRoleCode(RoleConstants.PLATFORM_ADMIN);
        user.setStatus(0);
        user.setDeleted(0L);

        when(sysUserMapper.selectByUsername("admin")).thenReturn(user);
        when(jwtTokenUtil.generateToken(any(User.class))).thenReturn("token-value");

        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("password");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("token-value", response.getToken());
        assertEquals("admin", response.getUsername());
        assertEquals("Admin", response.getRealName());
        assertEquals(RoleConstants.PLATFORM_ADMIN, response.getRoleCode());
    }

    @Test
    void shouldRejectDisabledUserWithStatusOne() {
        SysUser user = new SysUser();
        user.setUsername("disabled");
        user.setPassword(passwordEncoder.encode("password"));
        user.setStatus(1);
        user.setDeleted(0L);

        when(sysUserMapper.selectByUsername("disabled")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("disabled");
        request.setPassword("password");

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtTokenUtil, never()).generateToken(any());
    }
}
