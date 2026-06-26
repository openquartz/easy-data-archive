package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ArchiveTaskGroupNameResolver {

    private final ArchiveGroupMapper archiveGroupMapper;

    public void fillGroupNames(List<ArchiveGroupExecuteTask> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        Set<Long> groupIds = tasks.stream()
                .filter(Objects::nonNull)
                .map(ArchiveGroupExecuteTask::getGroupId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (groupIds.isEmpty()) {
            return;
        }
        Map<Long, ArchiveGroup> groupMap = archiveGroupMapper.selectByIds(groupIds).stream()
                .collect(Collectors.toMap(ArchiveGroup::getId, Function.identity()));
        tasks.stream().filter(Objects::nonNull).forEach(task -> {
            ArchiveGroup group = groupMap.get(task.getGroupId());
            if (group != null) {
                task.setGroupName(group.getGroupName());
            }
        });
    }
}
