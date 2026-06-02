package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.service.ArchiveDatasourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据源控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/archive/datasources")
@RequiredArgsConstructor
public class ArchiveDatasourceController {

    private final ArchiveDatasourceService datasourceService;

    @GetMapping
    public ApiResponse<List<ArchiveConnection>> getDatasources() {
        return ApiResponse.success(datasourceService.findAll());
    }

    @PostMapping
    public ApiResponse<ArchiveConnection> createDatasource(@RequestBody ArchiveConnection datasource) {
        return ApiResponse.success(datasourceService.create(datasource));
    }

    @GetMapping("/{id}")
    public ApiResponse<ArchiveConnection> getDatasource(@PathVariable Long id) {
        return ApiResponse.success(datasourceService.findById(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<ArchiveConnection> updateDatasource(@PathVariable Long id, @RequestBody ArchiveConnection datasource) {
        datasource.setId(id);
        return ApiResponse.success(datasourceService.update(datasource));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<?> updateDatasourceStatus(@PathVariable Long id, @RequestParam Integer status) {
        datasourceService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @PostMapping("/test")
    public ApiResponse<Boolean> testConnection(@RequestBody ArchiveConnection datasource) {
        boolean result = datasourceService.testConnection(datasource);
        return ApiResponse.success(result);
    }
}
