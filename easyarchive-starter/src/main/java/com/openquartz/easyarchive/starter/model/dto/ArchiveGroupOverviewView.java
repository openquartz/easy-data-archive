package com.openquartz.easyarchive.starter.model.dto;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import lombok.Data;

import java.util.List;

@Data
public class ArchiveGroupOverviewView {

    private ArchiveGroupView group;
    private ArchiveGroupItemStatsView itemStats;
    private ArchiveGroupTaskStatsView taskStats;
    private List<ArchiveGroupExecuteTask> recentTasks;
}
