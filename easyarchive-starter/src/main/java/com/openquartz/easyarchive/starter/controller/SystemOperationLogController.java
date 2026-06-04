package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryRequest;
import com.openquartz.easyarchive.starter.service.OperationLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/system/logs")
@RequiredArgsConstructor
public class SystemOperationLogController {

    private final OperationLogQueryService operationLogQueryService;

    @GetMapping
    public ApiResponse<Map<String, Object>> queryLogs(
            @ModelAttribute OperationLogQueryRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        page = Math.max(1, page);
        size = Math.min(Math.max(1, size), 500);
        return ApiResponse.success(operationLogQueryService.query(request, page, size));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleForbidden(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("FORBIDDEN", ex.getMessage()));
    }
}
