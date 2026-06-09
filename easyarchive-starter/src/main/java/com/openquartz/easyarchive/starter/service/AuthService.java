package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.model.dto.LoginRequest;
import com.openquartz.easyarchive.starter.model.dto.LoginResponse;

/**
 * 认证服务
 */
public interface AuthService {

    LoginResponse login(LoginRequest loginRequest);

    void logout();

    Object getCurrentUser();

    void changePassword(String newPassword);
}