package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.GrantUserDatasourcePermissionRequest;
import com.openquartz.easyarchive.starter.model.dto.ReplaceUserDatasourcePermissionsRequest;
import com.openquartz.easyarchive.starter.service.UserDatasourcePermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{userId}/datasource-permissions")
@RequiredArgsConstructor
public class UserDatasourcePermissionController {

    private final UserDatasourcePermissionService permissionService;

    @GetMapping
    public ApiResponse<List<ArchiveConnection>> list(@PathVariable Long userId) {
        return ApiResponse.success(permissionService.listUserPermissions(userId));
    }

    @PostMapping
    @OperationLog(value = "授予用户数据源权限", module = "USER_DATASOURCE_PERMISSION", action = "GRANT",
            button = "授予用户数据源权限")
    public ApiResponse<?> grant(@PathVariable Long userId,
                                @RequestBody GrantUserDatasourcePermissionRequest request) {
        permissionService.grant(userId, request.getDatasourceId());
        return ApiResponse.success();
    }

    @DeleteMapping("/{datasourceId}")
    @OperationLog(value = "撤销用户数据源权限", module = "USER_DATASOURCE_PERMISSION", action = "REVOKE",
            button = "撤销用户数据源权限")
    public ApiResponse<?> revoke(@PathVariable Long userId, @PathVariable Long datasourceId) {
        permissionService.revoke(userId, datasourceId);
        return ApiResponse.success();
    }

    @PutMapping
    @OperationLog(value = "替换用户数据源权限", module = "USER_DATASOURCE_PERMISSION", action = "REPLACE",
            button = "替换用户数据源权限")
    public ApiResponse<?> replace(@PathVariable Long userId,
                                  @RequestBody ReplaceUserDatasourcePermissionsRequest request) {
        permissionService.replacePermissions(userId, request.getDatasourceIds());
        return ApiResponse.success();
    }
}
