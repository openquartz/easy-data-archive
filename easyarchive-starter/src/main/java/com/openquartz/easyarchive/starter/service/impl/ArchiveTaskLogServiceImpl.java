package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.core.rule.entity.ArchiveTaskLog;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ArchiveTaskLogServiceImpl implements ArchiveTaskLogService {

    private final ArchiveLogRepository archiveLogRepository;

    @Override
    public Map<String, Object> queryTasks(int page, int size, String status) {
        List<ArchiveGroupExecuteTask> list = archiveLogRepository.queryTasks(page, size, status);
        int total = archiveLogRepository.countTasks(status);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public Object queryTaskById(Long taskId) {
        return archiveLogRepository.queryTaskById(taskId);
    }

    @Override
    public Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase) {
        List<ArchiveTaskLog> list = archiveLogRepository.queryLogsByTaskId(taskId, page, size, executePhase);
        int total = archiveLogRepository.countLogsByTaskId(taskId, executePhase);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    @Override
    public int cleanup(int retentionDays) {
        return archiveLogRepository.deleteByRetentionDays(retentionDays);
    }
}
