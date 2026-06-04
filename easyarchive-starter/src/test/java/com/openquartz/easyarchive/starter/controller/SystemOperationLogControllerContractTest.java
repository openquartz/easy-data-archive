package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.OperationLogQueryItem;
import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.OperationLogQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SystemOperationLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class SystemOperationLogControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OperationLogQueryService operationLogQueryService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnPagedOperationLogsWithStableEnvelope() throws Exception {
        OperationLogQueryItem item = new OperationLogQueryItem();
        item.setOperator("Alice");
        item.setModuleCode("ARCHIVE_GROUP");
        item.setButtonName("编辑分组");
        item.setBizIdentifier("group-1");
        item.setContent("编辑分组 archive");
        item.setResultStatus(0);
        item.setErrorMessage(null);

        Map<String, Object> result = new HashMap<>();
        result.put("list", Collections.singletonList(item));
        result.put("total", 1);
        result.put("page", 1);
        result.put("size", 20);
        when(operationLogQueryService.query(any(), eq(1), eq(20))).thenReturn(result);

        mockMvc.perform(get("/api/v1/system/logs")
                        .param("operator", "Alice")
                        .param("moduleCode", "ARCHIVE_GROUP")
                        .param("page", "1")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].operator").value("Alice"))
                .andExpect(jsonPath("$.data.list[0].moduleCode").value("ARCHIVE_GROUP"))
                .andExpect(jsonPath("$.data.list[0].buttonName").value("编辑分组"))
                .andExpect(jsonPath("$.data.list[0].bizIdentifier").value("group-1"))
                .andExpect(jsonPath("$.data.list[0].content").value("编辑分组 archive"))
                .andExpect(jsonPath("$.data.list[0].resultStatus").value(0));
    }

    @Test
    void shouldReturnForbiddenWhenRequesterIsNotAdmin() throws Exception {
        when(operationLogQueryService.query(any(), eq(1), eq(20))).thenThrow(new IllegalStateException("无管理员权限"));

        mockMvc.perform(get("/api/v1/system/logs")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("无管理员权限"));
    }
}
