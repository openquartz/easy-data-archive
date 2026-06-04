package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.common.enums.DatasourceStatusEnum;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_TASK_LIMIT = 10;
    private static final int FAILED_TASK_LIMIT = 10;
    private static final int TREND_DAYS = 7;
    private final ArchiveGroupExecuteTaskMapper archiveGroupExecuteTaskMapper;
    private final ArchiveConnectionMapper eaArchiveDatasourceMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("taskStatusCounts", archiveGroupExecuteTaskMapper.countByExecuteStatus());
        result.put("recentTasks", archiveGroupExecuteTaskMapper.selectRecentTasks(RECENT_TASK_LIMIT));
        result.put("failedTasks", archiveGroupExecuteTaskMapper.selectFailedTasks(FAILED_TASK_LIMIT));
        result.put("dailyTaskTrend", buildDailyTaskTrend());
        result.put("datasourceStatusSummary", buildDatasourceStatusSummary());
        return result;
    }

    private List<Map<String, Object>> buildDailyTaskTrend() {
        LocalDate endDay = LocalDate.now();
        LocalDate startDay = endDay.minusDays(TREND_DAYS - 1L);
        List<Map<String, Object>> rawTrend = archiveGroupExecuteTaskMapper
                .selectDailyTrend(startDay.toString(), endDay.toString());
        Map<String, Map<String, Object>> rawTrendMap = new HashMap<>();
        for (Map<String, Object> item : rawTrend) {
            Object day = item.get("day");
            if (day != null) {
                rawTrendMap.put(String.valueOf(day), item);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (LocalDate current = startDay; !current.isAfter(endDay); current = current.plusDays(1)) {
            String day = current.toString();
            Map<String, Object> item = rawTrendMap.get(day);
            Map<String, Object> trend = new LinkedHashMap<>();
            trend.put("day", day);
            trend.put("submittedCount", toInt(item == null ? null : item.get("submittedCount")));
            trend.put("successCount", toInt(item == null ? null : item.get("successCount")));
            trend.put("failedCount", toInt(item == null ? null : item.get("failedCount")));
            result.add(trend);
        }
        return result;
    }

    private Map<String, Object> buildDatasourceStatusSummary() {
        List<Map<String, Object>> statusCounts = eaArchiveDatasourceMapper.countByStatus();
        Map<String, Object> summary = new HashMap<>();
        summary.put("total", 0);
        summary.put("enabled", 0);
        summary.put("disabled", 0);
        summary.put("statusCounts", statusCounts);
        for (Map<String, Object> item : statusCounts) {
            Number status = (Number) item.get("status");
            Number count = (Number) item.get("count");
            if (count == null) {
                continue;
            }
            int countValue = count.intValue();
            summary.put("total", ((Integer) summary.get("total")) + countValue);
            if (status != null && DatasourceStatusEnum.isEnabled(status.intValue())) {
                summary.put("enabled", countValue);
            } else if (status != null && DatasourceStatusEnum.DISABLED.getCode().equals(status.intValue())) {
                summary.put("disabled", countValue);
            }
        }
        return summary;
    }

    private int toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }
}
