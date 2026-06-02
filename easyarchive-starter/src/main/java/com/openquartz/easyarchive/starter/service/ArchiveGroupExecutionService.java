package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;

public interface ArchiveGroupExecutionService {

    ArchiveGroupExecuteTask trigger(Long groupId);
}
