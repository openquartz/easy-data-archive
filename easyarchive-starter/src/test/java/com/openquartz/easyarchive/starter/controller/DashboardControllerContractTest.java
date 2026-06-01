package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnOverviewWithStableEnvelopeAndKeys() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskStatusCounts", Arrays.asList(
                mapOf("status", 0, "count", 1),
                mapOf("status", 1, "count", 2)
        ));
        payload.put("recentTasks", Arrays.asList(mapOf("id", 10, "groupId", 2)));
        payload.put("failedTasks", Arrays.asList(mapOf("id", 11, "groupId", 2)));
        payload.put("datasourceStatusSummary", mapOf("total", 5, "enabled", 3, "disabled", 2));

        when(dashboardService.getOverview()).thenReturn(payload);

        mockMvc.perform(get("/api/v1/dashboard/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data.taskStatusCounts").isArray())
                .andExpect(jsonPath("$.data.recentTasks").isArray())
                .andExpect(jsonPath("$.data.failedTasks").isArray())
                .andExpect(jsonPath("$.data.datasourceStatusSummary.total").value(5))
                .andExpect(jsonPath("$.data.taskStatusCounts[0].status").value(0));
    }

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return result;
    }
}
