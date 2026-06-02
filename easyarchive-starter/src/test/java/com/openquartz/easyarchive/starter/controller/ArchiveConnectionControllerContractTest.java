package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.DatasourceTypeOption;
import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.ArchiveConnectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ArchiveConnectionController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArchiveConnectionControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArchiveConnectionService archiveConnectionService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldListDatasourceTypesWithStableEnvelope() throws Exception {
        when(archiveConnectionService.listDatasourceTypes())
                .thenReturn(Collections.singletonList(new DatasourceTypeOption("MYSQL", "MySQL")));

        mockMvc.perform(get("/api/v1/archive/datasources/types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].code").value("MYSQL"))
                .andExpect(jsonPath("$.data[0].name").value("MySQL"));
    }
}
