package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.common.entity.Pair;
import com.openquartz.easyarchive.common.enums.ArchiveTaskStatusEnum;
import com.openquartz.easyarchive.common.enums.EnableStatusEnum;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.exception.StarterErrorCode;
import com.openquartz.easyarchive.starter.exception.StarterManageException;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupExecuteTaskMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveConnectionMapper;
import com.openquartz.easyarchive.starter.rule.PlatformArchiveRuleLoader;
import com.openquartz.easyarchive.starter.service.ArchiveGroupExecutionService;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.service.ArchiveTaskLogService;
import com.openquartz.easyarchive.starter.service.ArchiveResourceAccessService;
import com.openquartz.easyarchive.starter.support.ArchiveGroupTaskDispatcher;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class ArchiveGroupExecutionServiceImpl implements ArchiveGroupExecutionService {
    private static final long INITIAL_PROCESSED_RECORDS = 0L;
    private static final long INITIAL_FINISHED_FLAG = 0L;

    private final ArchiveGroupMapper groupMapper;
    private final ArchiveGroupItemByIdMapper idMapper;
    private final ArchiveGroupItemByTimeMapper timeMapper;
    private final ArchiveGroupExecuteTaskMapper taskMapper;
    private final ArchiveConnectionMapper datasourceMapper;
    private final ArchiveGroupTaskDispatcher dispatcher;
    private final ArchiveTaskLogService taskLogService;
    private final ArchiveResourceAccessService archiveResourceAccessService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveGroupExecuteTask trigger(Long groupId) {
        archiveResourceAccessService.assertGroupAccessible(groupId);
        return triggerGroupWithoutPermission(groupId);
    }

    private synchronized @NonNull ArchiveGroupExecuteTask triggerGroupWithoutPermission(Long groupId) {
        ArchiveGroup group = requireEnabledGroup(groupId);
        if (taskMapper.countActiveByGroupId(groupId) > 0) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_HAS_ACTIVE_TASK);
        }
        if (idMapper.countEnabledByGroupId(groupId) + timeMapper.countEnabledByGroupId(groupId) == 0) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_HAS_NO_ENABLED_ITEM);
        }

        ArchiveConnection source = requireDatasource(group.getSourceDatasourceId());
        ArchiveConnection target = requireDatasource(group.getTargetDatasourceId());

        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setGroupId(groupId);
        task.setStartTime(new Date());
        task.setExecuteStatus(ArchiveTaskStatusEnum.WAITING.getCode());
        task.setProcessedRecords(INITIAL_PROCESSED_RECORDS);
        task.setFinishedFlag(INITIAL_FINISHED_FLAG);
        taskMapper.insert(task);

        PlatformArchiveRuleLoader loader = new PlatformArchiveRuleLoader(groupId, idMapper, timeMapper);
        dispatcher.dispatch(loader, task, Pair.of(source, target));
        return task;
    }


    @Override
    public ArchiveGroupExecuteTask trigger(String groupCode) {
        String normalizedGroupCode = groupCode == null ? null : groupCode.trim();
        if (normalizedGroupCode == null || normalizedGroupCode.isEmpty()) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_CODE_REQUIRED);
        }
        ArchiveGroup group = groupMapper.selectByCode(normalizedGroupCode);
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
        }
        return triggerGroupWithoutPermission(group.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ArchiveGroupExecuteTask cancelActiveTask(Long groupId, String cancelReason) {
        archiveResourceAccessService.assertGroupAccessible(groupId);
        requireExistingGroup(groupId);
        ArchiveGroupExecuteTask activeTask = taskMapper.selectLatestActiveByGroupId(groupId);
        if (activeTask == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_HAS_NO_ACTIVE_TASK);
        }
        taskLogService.cancelTask(activeTask.getId(), cancelReason);
        return activeTask;
    }

    private ArchiveGroup requireEnabledGroup(Long groupId) {
        ArchiveGroup group = requireExistingGroup(groupId);
        if (!EnableStatusEnum.isEnabled(group.getEnableStatus())) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_DISABLED);
        }
        return group;
    }

    private ArchiveGroup requireExistingGroup(Long groupId) {
        if (groupId == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_ID_REQUIRED);
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOT_FOUND);
        }
        return group;
    }

    private ArchiveConnection requireDatasource(Long datasourceId) {
        if (datasourceId == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
        }
        Object datasource = datasourceMapper.selectById(datasourceId);
        if (datasource == null) {
            throw new StarterManageException(StarterErrorCode.DATASOURCE_NOT_FOUND);
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
            throw StarterManageException.withPlaceholders(
                StarterErrorCode.UNSUPPORTED_DATASOURCE_TYPE, source.getClass().getName());
        }
    }
}
