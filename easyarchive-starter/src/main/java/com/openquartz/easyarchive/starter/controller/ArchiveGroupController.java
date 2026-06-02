package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/archive/groups")
@RequiredArgsConstructor
public class ArchiveGroupController {

    private final ArchiveGroupService groupService;
    private final ArchiveGroupExecutionService executionService;

    @GetMapping
    public ApiResponse<List<ArchiveGroup>> list(@RequestParam(required = false) Integer enableStatus) {
        return ApiResponse.success(groupService.findAll(enableStatus));
    }

    @GetMapping("/tree")
    public ApiResponse<List<ArchiveGroup>> tree() {
        return ApiResponse.success(groupService.tree());
    }

    @GetMapping("/{id}")
    public ApiResponse<ArchiveGroup> detail(@PathVariable Long id) {
        return ApiResponse.success(groupService.findById(id));
    }

    @PostMapping
    public ApiResponse<ArchiveGroup> create(@RequestBody ArchiveGroup group) {
        return ApiResponse.success(groupService.create(group));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArchiveGroup> update(@PathVariable Long id, @RequestBody ArchiveGroup group) {
        group.setId(id);
        return ApiResponse.success(groupService.update(group));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateStatus(@PathVariable Long id, @RequestParam Integer enableStatus) {
        groupService.updateStatus(id, enableStatus);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable Long id) {
        groupService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/trigger")
    public ApiResponse<ArchiveGroupExecuteTask> trigger(@PathVariable Long id) {
        return ApiResponse.success(executionService.trigger(id));
    }
}
