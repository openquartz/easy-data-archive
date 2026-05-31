package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/task-log")
@RequiredArgsConstructor
public class ArchiveTaskLogController {

    private final ArchiveTaskLogService taskLogService;

    @GetMapping("/tasks")
    public ApiResponse<Map<String, Object>> getTasks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(taskLogService.queryTasks(page, size, status));
    }

    @GetMapping("/tasks/{taskId}")
    public ApiResponse<Object> getTask(@PathVariable Long taskId) {
        Object task = taskLogService.queryTaskById(taskId);
        if (task == null) {
            return ApiResponse.error("NOT_FOUND", "任务不存在");
        }
        return ApiResponse.success(task);
    }

    @GetMapping("/tasks/{taskId}/logs")
    public ApiResponse<Map<String, Object>> getTaskLogs(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String executePhase) {
        return ApiResponse.success(taskLogService.queryLogsByTaskId(taskId, page, size, executePhase));
    }

    @PostMapping("/cleanup")
    public ApiResponse<Integer> cleanup(@RequestParam(defaultValue = "30") int retentionDays) {
        int deleted = taskLogService.cleanup(retentionDays);
        log.info("[ArchiveTaskLogController] cleanup deleted {} records, retentionDays:{}", deleted, retentionDays);
        return ApiResponse.success(deleted);
    }
}
