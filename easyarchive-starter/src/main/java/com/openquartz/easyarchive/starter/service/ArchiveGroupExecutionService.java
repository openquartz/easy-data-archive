package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;

public interface ArchiveGroupExecutionService {

    /**
     * 触发
     * @param groupId 分组ID
     * @return 执行任务
     */
    ArchiveGroupExecuteTask trigger(Long groupId);

    /**
     * 触发归档
     * @param groupCode 分组编码
     * @return 归档
     */
    ArchiveGroupExecuteTask trigger(String groupCode);

    /**
     * 取消任务
     * @param groupId 归档分组ID
     * @param cancelReason 取消原因
     * @return 执行任务
     */
    ArchiveGroupExecuteTask cancelActiveTask(Long groupId, String cancelReason);
}
