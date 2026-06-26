package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.starter.model.dto.TaskVO;
import java.util.Map;

public interface ArchiveTaskLogService {

    Map<String, Object> queryTasks(int page, int size, String status, Long groupId);

    TaskVO queryTaskById(Long taskId);

    Map<String, Object> queryLogsByTaskId(Long taskId, int page, int size, String executePhase);

    int cleanup(int retentionDays);

    void cancelTask(Long taskId, String cancelReason);
}
