package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.EaArchiveDatasourceMapper;
import com.openquartz.easyarchive.starter.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final int RECENT_TASK_LIMIT = 10;
    private static final int FAILED_TASK_LIMIT = 10;

    private final ArchiveGroupExecuteTaskMapper archiveGroupExecuteTaskMapper;
    private final EaArchiveDatasourceMapper eaArchiveDatasourceMapper;

    @Override
    public Map<String, Object> getOverview() {
        Map<String, Object> result = new HashMap<>();
        result.put("taskStatusCounts", archiveGroupExecuteTaskMapper.countByExecuteStatus());
        result.put("recentTasks", archiveGroupExecuteTaskMapper.selectRecentTasks(RECENT_TASK_LIMIT));
        result.put("failedTasks", archiveGroupExecuteTaskMapper.selectFailedTasks(FAILED_TASK_LIMIT));
        result.put("datasourceStatusSummary", buildDatasourceStatusSummary());
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
            if (status != null && status.intValue() == 1) {
                summary.put("enabled", countValue);
            } else if (status != null && status.intValue() == 0) {
                summary.put("disabled", countValue);
            }
        }
        return summary;
    }
}
