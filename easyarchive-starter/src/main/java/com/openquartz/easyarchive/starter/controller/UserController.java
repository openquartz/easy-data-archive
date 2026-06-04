package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<List<SysUser>> getUsers() {
        return ApiResponse.success(userService.findAll());
    }

    @PostMapping
    @OperationLog(value = "新增用户", module = "USER", action = "CREATE", button = "新增用户")
    public ApiResponse<SysUser> createUser(@RequestBody SysUser user) {
        return ApiResponse.success(userService.create(user));
    }

    @PutMapping("/{id}")
    @OperationLog(value = "编辑用户", module = "USER", action = "UPDATE", button = "编辑用户")
    public ApiResponse<SysUser> updateUser(@PathVariable Long id, @RequestBody SysUser user) {
        user.setId(id);
        return ApiResponse.success(userService.update(user));
    }

    @PatchMapping("/{id}/status")
    @OperationLog(value = "修改用户状态", module = "USER", action = "STATUS", button = "修改用户状态")
    public ApiResponse<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<SysUser> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.findById(id));
    }
}
