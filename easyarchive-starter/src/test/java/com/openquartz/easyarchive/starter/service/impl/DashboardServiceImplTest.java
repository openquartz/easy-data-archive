package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceImplTest {

    private final ArchiveGroupExecuteTaskMapper taskMapper = mock(ArchiveGroupExecuteTaskMapper.class);
    private final ArchiveConnectionMapper datasourceMapper = mock(ArchiveConnectionMapper.class);
    private final DashboardServiceImpl service = new DashboardServiceImpl(taskMapper, datasourceMapper);

    @Test
    void shouldBuildSevenDayTrendAndFillMissingDates() {
        when(taskMapper.countByExecuteStatus()).thenReturn(Arrays.asList(mapOf("status", 2, "count", 3)));
        when(taskMapper.selectRecentTasks(10)).thenReturn(List.of());
        when(taskMapper.selectFailedTasks(10)).thenReturn(List.of());
        when(datasourceMapper.countByStatus()).thenReturn(Arrays.asList(
                mapOf("status", 1, "count", 2),
                mapOf("status", 0, "count", 1)
        ));

        LocalDate today = LocalDate.now();
        when(taskMapper.selectDailyTrend(today.minusDays(6).toString(), today.toString())).thenReturn(Arrays.asList(
                mapOf("day", today.minusDays(6).toString(), "submittedCount", 4, "successCount", 2, "failedCount", 1),
                mapOf("day", today.minusDays(4).toString(), "submittedCount", 3, "successCount", 1, "failedCount", 0),
                mapOf("day", today.toString(), "submittedCount", 2, "successCount", 0, "failedCount", 1)
        ));

        Map<String, Object> overview = service.getOverview();

        Object trendObject = overview.get("dailyTaskTrend");
        assertNotNull(trendObject);
        assertInstanceOf(List.class, trendObject);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) trendObject;
        assertEquals(7, trend.size());

        assertEquals(today.minusDays(6).toString(), trend.get(0).get("day"));
        assertEquals(4, trend.get(0).get("submittedCount"));
        assertEquals(2, trend.get(0).get("successCount"));
        assertEquals(1, trend.get(0).get("failedCount"));

        assertEquals(today.minusDays(5).toString(), trend.get(1).get("day"));
        assertEquals(0, trend.get(1).get("submittedCount"));
        assertEquals(0, trend.get(1).get("successCount"));
        assertEquals(0, trend.get(1).get("failedCount"));

        assertEquals(today.toString(), trend.get(6).get("day"));
        assertEquals(2, trend.get(6).get("submittedCount"));
        assertEquals(0, trend.get(6).get("successCount"));
        assertEquals(1, trend.get(6).get("failedCount"));
    }

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> result = new HashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            result.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return result;
    }
}
