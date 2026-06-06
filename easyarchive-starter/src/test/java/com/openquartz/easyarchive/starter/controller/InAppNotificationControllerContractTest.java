package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.starter.model.dto.InAppNotificationUnreadCountView;
import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.InAppNotificationQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InAppNotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class InAppNotificationControllerContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InAppNotificationQueryService queryService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    void shouldReturnUnreadCountForCurrentUser() throws Exception {
        when(queryService.getUnreadCount()).thenReturn(new InAppNotificationUnreadCountView(3));

        mockMvc.perform(get("/api/v1/in-app-notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.unreadCount").value(3));
    }

    @Test
    void shouldReturnNotificationList() throws Exception {
        mockMvc.perform(get("/api/v1/in-app-notifications?limit=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(queryService).listLatest(10);
    }

    @Test
    void shouldMarkNotificationRead() throws Exception {
        mockMvc.perform(post("/api/v1/in-app-notifications/101/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(queryService).markRead(101L);
    }

    @Test
    void shouldMarkAllNotificationsRead() throws Exception {
        mockMvc.perform(post("/api/v1/in-app-notifications/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));

        verify(queryService).markAllRead();
    }
}
