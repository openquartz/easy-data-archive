package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.mapper.SysOperationLogMapper;
import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryItem;
import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryRequest;
import com.openquartz.easyarchive.starter.service.DataPermissionService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationLogQueryServiceImplTest {

    private final SysOperationLogMapper sysOperationLogMapper = mock(SysOperationLogMapper.class);
    private final DataPermissionService dataPermissionService = mock(DataPermissionService.class);
    private final OperationLogQueryServiceImpl service = new OperationLogQueryServiceImpl(
            sysOperationLogMapper, dataPermissionService);

    @Test
    void shouldRequireAdminAndQueryPagedLogs() {
        OperationLogQueryRequest request = new OperationLogQueryRequest();
        request.setStartTime(new Date(1717400000000L));
        request.setEndTime(new Date(1717486400000L));
        request.setOperator("alice");
        request.setModuleCode("ARCHIVE_GROUP");
        request.setResultStatus(1);

        OperationLogQueryItem item = new OperationLogQueryItem();
        item.setOperator("Alice");
        item.setModuleCode("ARCHIVE_GROUP");
        when(sysOperationLogMapper.selectPage(request, 20, 10)).thenReturn(Collections.singletonList(item));
        when(sysOperationLogMapper.count(request)).thenReturn(1);
        doNothing().when(dataPermissionService).assertAdmin();

        Map<String, Object> result = service.query(request, 3, 10);

        assertEquals(1, result.get("total"));
        assertEquals(3, result.get("page"));
        assertEquals(10, result.get("size"));
        verify(dataPermissionService).assertAdmin();
        verify(sysOperationLogMapper).selectPage(request, 20, 10);
        verify(sysOperationLogMapper).count(request);
    }
}
