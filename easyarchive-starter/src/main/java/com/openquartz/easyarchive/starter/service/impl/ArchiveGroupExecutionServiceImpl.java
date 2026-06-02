package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
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
    private final ArchiveConnectionMapper datasourceMapper;
    private final ArchiveGroupTaskDispatcher dispatcher;

    @Override
    public synchronized ArchiveGroupExecuteTask trigger(Long groupId) {
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
        Object datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new IllegalArgumentException(message);
        }
        return toArchiveConnection(datasource);
    }

    private ArchiveConnection toArchiveConnection(Object datasource) {
        if (datasource instanceof ArchiveConnection) {
            return (ArchiveConnection) datasource;
        }

        ArchiveConnection connection = new ArchiveConnection();
        connection.setId(readLong(datasource, "getId"));
        connection.setConnectCode(readString(datasource, "getDatasourceCode"));
        connection.setConnectType(readString(datasource, "getDatasourceType"));
        connection.setUrl(readString(datasource, "getJdbcUrl"));
        connection.setUsername(readString(datasource, "getUsername"));
        connection.setPassword(readString(datasource, "getPasswordCipher"));
        connection.setStatus(readInteger(datasource, "getStatus"));
        connection.setRemark(readString(datasource, "getRemark"));
        return connection;
    }

    private String readString(Object source, String methodName) {
        Object value = invokeGetter(source, methodName);
        return value == null ? null : String.valueOf(value);
    }

    private Long readLong(Object source, String methodName) {
        Object value = invokeGetter(source, methodName);
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private Integer readInteger(Object source, String methodName) {
        Object value = invokeGetter(source, methodName);
        return value instanceof Number ? ((Number) value).intValue() : null;
    }

    private Object invokeGetter(Object source, String methodName) {
        try {
            return source.getClass().getMethod(methodName).invoke(source);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException("Unsupported datasource type: " + source.getClass().getName(), ex);
        }
    }
}
