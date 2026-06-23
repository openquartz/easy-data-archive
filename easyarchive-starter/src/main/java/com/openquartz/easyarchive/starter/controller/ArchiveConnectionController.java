package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.service.ArchiveConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
@Slf4j
@RestController
@RequestMapping("/api/v1/archive/datasources")
@RequiredArgsConstructor
public class ArchiveConnectionController {

    private final ArchiveConnectionService datasourceService;

    @GetMapping("/types")
    public ApiResponse<List<DatasourceTypeOption>> getDatasourceTypes() {
        return ApiResponse.success(datasourceService.listDatasourceTypes());
    }

    @GetMapping
    public ApiResponse<List<ArchiveConnection>> getDatasources() {
        return ApiResponse.success(datasourceService.findAll());
    }

    @PostMapping
    @OperationLog(value = "新增数据源", module = "DATASOURCE", action = "CREATE", button = "新增数据源")
    public ApiResponse<ArchiveConnection> createDatasource(@Valid @RequestBody ArchiveConnection datasource) {
        return ApiResponse.success(datasourceService.create(datasource));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArchiveConnection>> getDatasource(@PathVariable Long id) {
        ArchiveConnection ds = datasourceService.findById(id);
        if (ds == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("RESOURCE_NOT_FOUND", "归档连接不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(ds));
    }

    @PutMapping("/{id}")
    @OperationLog(value = "编辑数据源", module = "DATASOURCE", action = "UPDATE", button = "编辑数据源")
    public ApiResponse<ArchiveConnection> updateDatasource(@PathVariable Long id, @RequestBody ArchiveConnection datasource) {
        datasource.setId(id);
        return ApiResponse.success(datasourceService.update(datasource));
    }

    @PatchMapping("/{id}/status")
    @OperationLog(value = "修改数据源状态", module = "DATASOURCE", action = "STATUS", button = "修改数据源状态")
    public ApiResponse<?> updateDatasourceStatus(@PathVariable Long id, @RequestParam Integer status) {
        datasourceService.updateStatus(id, status);
        return ApiResponse.success();
    }

    @PostMapping("/test")
    @OperationLog(value = "测试连接", module = "DATASOURCE", action = "TEST", button = "测试连接")
    public ApiResponse<Boolean> testConnection(@RequestBody ArchiveConnection datasource) {
        boolean result = datasourceService.testConnection(datasource);
        return ApiResponse.success(result);
    }
}
