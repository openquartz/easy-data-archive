package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.starter.annotation.OperationLog;
import com.openquartz.easyarchive.starter.converter.DatasourceConverter;
import com.openquartz.easyarchive.starter.model.dto.ApiResponse;
import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.model.dto.PageResult;
import com.openquartz.easyarchive.starter.model.request.DatasourceCreateRequest;
import com.openquartz.easyarchive.starter.model.request.DatasourceUpdateRequest;
import com.openquartz.easyarchive.starter.model.vo.DatasourceVO;
import com.openquartz.easyarchive.starter.service.ArchiveConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/archive/datasources")
@RequiredArgsConstructor
public class ArchiveConnectionController {

    private final ArchiveConnectionService datasourceService;
    private final DatasourceConverter datasourceConverter;

    @GetMapping("/types")
    public ApiResponse<List<DatasourceTypeOption>> getDatasourceTypes() {
        return ApiResponse.success(datasourceService.listDatasourceTypes());
    }

    @GetMapping
    public ApiResponse<List<DatasourceVO>> getDatasources() {
        return ApiResponse.success(datasourceService.findAll().stream()
                .map(datasourceConverter::toVO)
                .collect(Collectors.toList()));
    }

    @GetMapping("/page")
    public ApiResponse<Map<String, Object>> getDatasourcesPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        page = Math.max(1, page);
        size = Math.min(Math.max(1, size), 500);
        PageResult<ArchiveConnection> result = datasourceService.findPage(page, size, keyword, status);
        List<DatasourceVO> voList = result.getData().stream()
                .map(datasourceConverter::toVO)
                .collect(Collectors.toList());
        Map<String, Object> response = new HashMap<>();
        response.put("data", voList);
        response.put("total", result.getTotal());
        response.put("page", result.getPage());
        response.put("size", result.getSize());
        return ApiResponse.success(response);
    }

    @PostMapping
    @OperationLog(value = "新增数据源", module = "DATASOURCE", action = "CREATE", button = "新增数据源")
    public ApiResponse<DatasourceVO> createDatasource(@Valid @RequestBody DatasourceCreateRequest request) {
        ArchiveConnection entity = datasourceConverter.toEntity(request);
        return ApiResponse.success(datasourceConverter.toVO(datasourceService.create(entity)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DatasourceVO>> getDatasource(@PathVariable Long id) {
        ArchiveConnection ds = datasourceService.findById(id);
        if (ds == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("RESOURCE_NOT_FOUND", "归档连接不存在"));
        }
        return ResponseEntity.ok(ApiResponse.success(datasourceConverter.toVO(ds)));
    }

    @PutMapping("/{id}")
    @OperationLog(value = "编辑数据源", module = "DATASOURCE", action = "UPDATE", button = "编辑数据源")
    public ApiResponse<DatasourceVO> updateDatasource(@PathVariable Long id, @Valid @RequestBody DatasourceUpdateRequest request) {
        ArchiveConnection entity = datasourceConverter.toEntity(request);
        entity.setId(id);
        return ApiResponse.success(datasourceConverter.toVO(datasourceService.update(entity)));
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
