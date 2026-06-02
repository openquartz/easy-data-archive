package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.ArchiveGroupExecutionService;
import com.openquartz.easyarchive.starter.service.ArchiveGroupService;
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

@WebMvcTest(controllers = ArchiveGroupController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArchiveGroupControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArchiveGroupService groupService;

    @MockBean
    private ArchiveGroupExecutionService executionService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldListArchiveGroupsWithStableEnvelope() throws Exception {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        when(groupService.findAll(null)).thenReturn(Collections.singletonList(group));

        mockMvc.perform(get("/api/v1/archive/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].groupCode").value("ORDER_ARCHIVE"));
    }
}
