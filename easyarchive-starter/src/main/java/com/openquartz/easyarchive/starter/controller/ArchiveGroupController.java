package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.PageResult;
import com.openquartz.easyarchive.starter.model.dto.UpdateOwnerRequest;
import com.openquartz.easyarchive.starter.service.ArchiveGroupExecutionService;
import com.openquartz.easyarchive.starter.service.ArchiveGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/archive/groups")
@RequiredArgsConstructor
public class ArchiveGroupController {

    private final ArchiveGroupService groupService;
    private final ArchiveGroupExecutionService executionService;

    @GetMapping
    public ApiResponse<List<ArchiveGroupView>> list(@RequestParam(required = false) Integer enableStatus) {
        return ApiResponse.success(groupService.findAll(enableStatus));
    }

    @GetMapping("/page")
    public ApiResponse<PageResult<ArchiveGroupView>> page(
            @RequestParam(required = false) Integer enableStatus,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(groupService.findPage(enableStatus, page, size));
    }

    @GetMapping("/tree")
    public ApiResponse<List<ArchiveGroup>> tree() {
        return ApiResponse.success(groupService.tree());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArchiveGroupView> detail(@PathVariable Long id) {
        return ApiResponse.success(groupService.findById(id));
    }

    @GetMapping("/{id}/overview")
    public ApiResponse<ArchiveGroupOverviewView> overview(@PathVariable Long id) {
        return ApiResponse.success(groupService.findOverview(id));
    }

    @PostMapping
    @OperationLog(value = "新增分组", module = "ARCHIVE_GROUP", action = "CREATE", button = "新增分组")
    public ApiResponse<ArchiveGroup> create(@RequestBody ArchiveGroup group) {
        return ApiResponse.success(groupService.create(group));
    }

    @PutMapping("/{id}")
    @OperationLog(value = "保存分组", module = "ARCHIVE_GROUP", action = "UPDATE", button = "保存分组")
    public ApiResponse<ArchiveGroup> update(@PathVariable Long id, @RequestBody ArchiveGroup group) {
        group.setId(id);
        return ApiResponse.success(groupService.update(group));
    }

    @PatchMapping("/{id}/status")
    @OperationLog(value = "修改分组状态", module = "ARCHIVE_GROUP", action = "STATUS", button = "修改分组状态")
    public ApiResponse<?> updateStatus(@PathVariable Long id, @RequestParam Integer enableStatus) {
        groupService.updateStatus(id, enableStatus);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @OperationLog(value = "删除分组", module = "ARCHIVE_GROUP", action = "DELETE", button = "删除分组")
    public ApiResponse<?> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/owner")
    @OperationLog(value = "变更负责人", module = "ARCHIVE_GROUP", action = "UPDATE_OWNER", button = "变更负责人")
    public ApiResponse<ArchiveGroup> updateOwner(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateOwnerRequest request) {
        return ApiResponse.success(groupService.updateOwner(id, request.getNewOwnerUserId()));
    }

    @PostMapping("/{id}/trigger")
    @OperationLog(value = "触发归档分组", module = "ARCHIVE_GROUP", action = "TRIGGER", button = "触发归档分组")
    public ApiResponse<ArchiveGroupExecuteTask> trigger(@PathVariable Long id) {
        return ApiResponse.success(executionService.trigger(id));
    }

    @PostMapping("/{id}/cancel-active-task")
    @OperationLog(value = "取消运行任务", module = "ARCHIVE_GROUP", action = "CANCEL_TASK", button = "取消运行任务")
    public ApiResponse<ArchiveGroupExecuteTask> cancelActiveTask(@PathVariable Long id,
                                                                 @RequestBody(required = false) Map<String, String> body) {
        String reason = body == null ? null : body.get("cancelReason");
        return ApiResponse.success(executionService.cancelActiveTask(id, reason));
    }
}
