package com.openquartz.easyarchive.starter.service;

import java.util.Map;

public interface ArchiveTaskLogService {

    Map<String, Object> queryTasks(int page, int size, String status);

    Object queryTaskById(Long taskId);

    Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase);

    int cleanup(int retentionDays);
}
