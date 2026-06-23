package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.converter.ArchiveGroupItemByIdConverter;
import com.openquartz.easyarchive.starter.converter.ArchiveGroupItemByTimeConverter;
import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.ArchiveGroupItemByIdService;
import com.openquartz.easyarchive.starter.service.ArchiveGroupItemByTimeService;
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

@WebMvcTest(controllers = ArchiveGroupItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ArchiveGroupItemControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArchiveGroupItemByIdService idService;

    @MockBean
    private ArchiveGroupItemByTimeService timeService;

    @MockBean
    private ArchiveGroupItemByIdConverter archiveGroupItemByIdConverter;

    @MockBean
    private ArchiveGroupItemByTimeConverter archiveGroupItemByTimeConverter;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldListArchiveGroupItemsWithStableEnvelope() throws Exception {
        ArchiveGroupItemById idItem = new ArchiveGroupItemById();
        idItem.setId(1L);
        idItem.setGroupId(10L);
        idItem.setSourceTable("t_order");
        idItem.setTargetTable("t_order_archive");
        idItem.setPriority(20);
        idItem.setStartId("1000");
        idItem.setEndId("2000");

        ArchiveGroupItemByTime timeItem = new ArchiveGroupItemByTime();
        timeItem.setId(2L);
        timeItem.setGroupId(10L);
        timeItem.setSourceTable("t_log");
        timeItem.setTargetTable("t_log_archive");
        timeItem.setPriority(10);
        timeItem.setStartTime(new java.util.Date(1704067200000L));
        timeItem.setKeepDay(30);

        when(idService.findByGroupId(10L, null)).thenReturn(Collections.singletonList(idItem));
        when(timeService.findByGroupId(10L, null)).thenReturn(Collections.singletonList(timeItem));

        mockMvc.perform(get("/api/v1/archive/groups/10/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].itemType").value("TIME"))
                .andExpect(jsonPath("$.data[0].rangeStart").value("2024-01-01 08:00:00"))
                .andExpect(jsonPath("$.data[0].rangeEnd").isString())
                .andExpect(jsonPath("$.data[1].itemType").value("ID"))
                .andExpect(jsonPath("$.data[1].rangeStart").value("1000"))
                .andExpect(jsonPath("$.data[1].rangeEnd").value("2000"));
    }
}
