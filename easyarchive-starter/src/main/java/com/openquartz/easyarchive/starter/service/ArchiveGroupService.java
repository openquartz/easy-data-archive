package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;

import java.util.List;

/**
 * 归档分组服务
 */
public interface ArchiveGroupService {

    List<ArchiveGroupView> findAll(Integer enableStatus);

    List<ArchiveGroup> tree();

    ArchiveGroupView findById(Long id);

    ArchiveGroupOverviewView findOverview(Long id);

    ArchiveGroup create(ArchiveGroup group);

    ArchiveGroup update(ArchiveGroup group);

    void updateStatus(Long id, Integer enableStatus);

    void delete(Long id);
}
