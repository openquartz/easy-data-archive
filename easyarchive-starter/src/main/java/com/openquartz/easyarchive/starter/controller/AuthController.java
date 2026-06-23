package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.ChangePasswordRequest;
import com.openquartz.easyarchive.starter.model.dto.LoginRequest;
import com.openquartz.easyarchive.starter.model.dto.LoginResponse;
import com.openquartz.easyarchive.starter.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ApiResponse.success(response);
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<?> getCurrentUser() {
        return ApiResponse.success(authService.getCurrentUser());
    }

    @PostMapping("/change-password")
    public ApiResponse<?> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request.getNewPassword());
        return ApiResponse.success();
    }
}