package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ArchiveGroupOverviewView {

    private ArchiveGroupView group;
    private ArchiveGroupItemStatsView itemStats;
    private ArchiveGroupTaskStatsView taskStats;
    private List<RecentTaskVO> recentTasks;
}
