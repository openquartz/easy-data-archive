package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.mapper.SysOperationLogMapper;
import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryRequest;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import com.openquartz.easyarchive.starter.service.OperationLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OperationLogQueryServiceImpl implements OperationLogQueryService {

    private final SysOperationLogMapper sysOperationLogMapper;
    private final DataPermissionService dataPermissionService;

    @Override
    public Map<String, Object> query(OperationLogQueryRequest request, int page, int size) {
        dataPermissionService.assertAdmin();
        int offset = (page - 1) * size;

        Map<String, Object> result = new HashMap<>();
        result.put("list", sysOperationLogMapper.selectPage(request, offset, size));
        result.put("total", sysOperationLogMapper.count(request));
        result.put("page", page);
        result.put("size", size);
        return result;
    }
}
