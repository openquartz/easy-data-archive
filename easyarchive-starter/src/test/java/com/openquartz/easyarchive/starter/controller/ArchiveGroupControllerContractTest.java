package com.openquartz.easyarchive.starter.controller;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupItemStatsView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupTaskStatsView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.model.dto.RecentTaskVO;
import com.openquartz.easyarchive.starter.security.JwtAuthenticationEntryPoint;
import com.openquartz.easyarchive.starter.security.JwtTokenUtil;
import com.openquartz.easyarchive.starter.service.ArchiveGroupExecutionService;
import com.openquartz.easyarchive.starter.service.ArchiveGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        ArchiveGroupView group = new ArchiveGroupView();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        group.setOwnerUserId(9L);
        group.setOwnerDisplayName("系统管理员 (admin)");
        group.setNotifyEnabled(1);
        group.setNotifyChannel("IN_APP");
        group.setActiveTaskId(88L);
        group.setActiveTaskStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
        group.setActiveTaskProcessedRecords(1234L);
        group.setActiveTaskProcessedSpeed(new BigDecimal("56.78"));
        group.setActiveTaskHeartbeatTime(new Date(1704067200000L));
        group.setCanTrigger(false);
        group.setCanCancelActiveTask(true);
        when(groupService.findAll(null)).thenReturn(Collections.singletonList(group));

        mockMvc.perform(get("/api/v1/archive/groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].groupCode").value("ORDER_ARCHIVE"))
                .andExpect(jsonPath("$.data[0].ownerUserId").value(9))
                .andExpect(jsonPath("$.data[0].ownerDisplayName").value("系统管理员 (admin)"))
                .andExpect(jsonPath("$.data[0].notifyEnabled").value(1))
                .andExpect(jsonPath("$.data[0].notifyChannel").value("IN_APP"))
                .andExpect(jsonPath("$.data[0].activeTaskId").value(88))
                .andExpect(jsonPath("$.data[0].activeTaskStatus").value(ArchiveGroupExecuteTask.STATUS_RUNNING))
                .andExpect(jsonPath("$.data[0].activeTaskProcessedRecords").value(1234))
                .andExpect(jsonPath("$.data[0].activeTaskProcessedSpeed").value(56.78))
                .andExpect(jsonPath("$.data[0].activeTaskHeartbeatTime").value("2024-01-01T00:00:00.000+00:00"))
                .andExpect(jsonPath("$.data[0].canTrigger").value(false))
                .andExpect(jsonPath("$.data[0].canCancelActiveTask").value(true));
    }

    @Test
    void shouldCancelActiveTaskFromGroupEndpoint() throws Exception {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(88L);
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLING);
        when(executionService.cancelActiveTask(eq(10L), any())).thenReturn(task);

        mockMvc.perform(post("/api/v1/archive/groups/10/cancel-active-task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cancelReason\":\"manual stop\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value(88))
                .andExpect(jsonPath("$.data.executeStatus").value(ArchiveGroupExecuteTask.STATUS_CANCELLING));
    }

    @Test
    void shouldGetArchiveGroupOverviewWithStableEnvelope() throws Exception {
        ArchiveGroupView group = new ArchiveGroupView();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        group.setOwnerUserId(9L);
        group.setOwnerDisplayName("系统管理员 (admin)");
        group.setActiveTaskProcessedRecords(1234L);
        group.setActiveTaskProcessedSpeed(new BigDecimal("56.78"));
        group.setActiveTaskHeartbeatTime(new Date(1704067200000L));

        ArchiveGroupItemStatsView itemStats = new ArchiveGroupItemStatsView();
        itemStats.setTotalCount(6L);
        itemStats.setEnabledCount(4L);
        itemStats.setDisabledCount(2L);
        itemStats.setIdTypeCount(3L);
        itemStats.setTimeTypeCount(3L);

        ArchiveGroupTaskStatsView taskStats = new ArchiveGroupTaskStatsView();
        taskStats.setTotalCount(20L);
        taskStats.setSuccessCount(15L);
        taskStats.setFailedCount(2L);
        taskStats.setRunningCount(1L);
        taskStats.setLastExecuteStatus(ArchiveGroupExecuteTask.STATUS_SUCCESS);
        taskStats.setLastExecuteTime(1704067200000L);

        RecentTaskVO recentTask = new RecentTaskVO();
        recentTask.setId(99L);
        recentTask.setGroupId(10L);
        recentTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);

        ArchiveGroupOverviewView overview = new ArchiveGroupOverviewView();
        overview.setGroup(group);
        overview.setItemStats(itemStats);
        overview.setTaskStats(taskStats);
        overview.setRecentTasks(Collections.singletonList(recentTask));

        when(groupService.findOverview(10L)).thenReturn(overview);

        mockMvc.perform(get("/api/v1/archive/groups/10/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.group.id").value(10))
                .andExpect(jsonPath("$.data.group.groupCode").value("ORDER_ARCHIVE"))
                .andExpect(jsonPath("$.data.group.ownerUserId").value(9))
                .andExpect(jsonPath("$.data.group.ownerDisplayName").value("系统管理员 (admin)"))
                .andExpect(jsonPath("$.data.group.activeTaskProcessedRecords").value(1234))
                .andExpect(jsonPath("$.data.group.activeTaskProcessedSpeed").value(56.78))
                .andExpect(jsonPath("$.data.group.activeTaskHeartbeatTime").value("2024-01-01T00:00:00.000+00:00"))
                .andExpect(jsonPath("$.data.itemStats.totalCount").value(6))
                .andExpect(jsonPath("$.data.itemStats.idTypeCount").value(3))
                .andExpect(jsonPath("$.data.taskStats.totalCount").value(20))
                .andExpect(jsonPath("$.data.taskStats.lastExecuteStatus").value(ArchiveGroupExecuteTask.STATUS_SUCCESS))
                .andExpect(jsonPath("$.data.taskStats.lastExecuteTime").value(1704067200000L))
                .andExpect(jsonPath("$.data.recentTasks").isArray())
                .andExpect(jsonPath("$.data.recentTasks[0].id").value(99))
                .andExpect(jsonPath("$.data.recentTasks[0].groupId").value(10));
    }

}
