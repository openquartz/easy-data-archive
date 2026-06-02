package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.EaArchiveDatasourceMapper;
import com.openquartz.easyarchive.starter.rule.PlatformArchiveRuleLoader;
import com.openquartz.easyarchive.starter.service.ArchiveGroupExecutionService;
import com.openquartz.easyarchive.starter.support.ArchiveGroupTaskDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class ArchiveGroupExecutionServiceImpl implements ArchiveGroupExecutionService {

    private final ArchiveGroupMapper groupMapper;
    private final ArchiveGroupItemByIdMapper idMapper;
    private final ArchiveGroupItemByTimeMapper timeMapper;
    private final ArchiveGroupExecuteTaskMapper taskMapper;
    private final EaArchiveDatasourceMapper datasourceMapper;
    private final ArchiveGroupTaskDispatcher dispatcher;

    @Override
    public ArchiveGroupExecuteTask trigger(Long groupId) {
        ArchiveGroup group = requireEnabledGroup(groupId);
        if (taskMapper.countActiveByGroupId(groupId) > 0) {
            throw new IllegalStateException("Archive group has active task");
        }
        if (idMapper.countEnabledByGroupId(groupId) + timeMapper.countEnabledByGroupId(groupId) == 0) {
            throw new IllegalStateException("Archive group has no enabled item");
        }

        ArchiveConnection source = requireDatasource(group.getSourceDatasourceId(), "source datasource not found");
        ArchiveConnection target = requireDatasource(group.getTargetDatasourceId(), "target datasource not found");

        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setGroupId(groupId);
        task.setStartTime(new Date());
        task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_WAITING);
        task.setProcessedRecords(0L);
        task.setFinishedFlag(0L);
        taskMapper.insert(task);

        PlatformArchiveRuleLoader loader = new PlatformArchiveRuleLoader(groupId, idMapper, timeMapper);
        dispatcher.dispatch(loader, task, Pair.of(source, target));
        return task;
    }

    private ArchiveGroup requireEnabledGroup(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("groupId is required");
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Archive group not found");
        }
        if (group.getEnableStatus() == null || group.getEnableStatus() != 0) {
            throw new IllegalStateException("Archive group is disabled");
        }
        return group;
    }

    private ArchiveConnection requireDatasource(Long datasourceId, String message) {
        if (datasourceId == null) {
            throw new IllegalArgumentException(message);
        }
        ArchiveConnection datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new IllegalArgumentException(message);
        }
        return datasource;
    }
}
