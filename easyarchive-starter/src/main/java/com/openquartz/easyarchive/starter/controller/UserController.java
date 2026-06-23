package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.common.SysUser;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.converter.UserConverter;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.request.UserCreateRequest;
import com.openquartz.easyarchive.starter.model.request.UserUpdateRequest;
import com.openquartz.easyarchive.starter.model.vo.UserVO;
import com.openquartz.easyarchive.starter.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserConverter userConverter;

    @GetMapping
    public ApiResponse<List<UserVO>> getUsers() {
        return ApiResponse.success(userService.findAll().stream()
                .map(userConverter::toVO)
                .collect(Collectors.toList()));
    }

    @PostMapping
    @OperationLog(value = "新增用户", module = "USER", action = "CREATE", button = "新增用户")
    public ApiResponse<UserVO> createUser(@Valid @RequestBody UserCreateRequest request) {
        SysUser entity = userConverter.toEntity(request);
        return ApiResponse.success(userConverter.toVO(userService.create(entity)));
    }

    @PutMapping("/{id}")
    @OperationLog(value = "编辑用户", module = "USER", action = "UPDATE", button = "编辑用户")
    public ApiResponse<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        SysUser entity = userConverter.toEntity(request);
        entity.setId(id);
        return ApiResponse.success(userConverter.toVO(userService.update(entity)));
    }

    @PatchMapping("/{id}/status")
    @OperationLog(value = "修改用户状态", module = "USER", action = "STATUS", button = "修改用户状态")
    public ApiResponse<?> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        userService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserVO>> getUser(@PathVariable Long id) {
        SysUser user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("RESOURCE_NOT_FOUND", "用户不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(userConverter.toVO(user)));
    }
}
