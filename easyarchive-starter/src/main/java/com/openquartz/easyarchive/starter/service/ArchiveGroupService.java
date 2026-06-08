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

    /**
     * 变更归档分组负责人
     *
     * @param groupId 分组ID
     * @param newOwnerUserId 新负责人ID
     * @return 更新后的分组
     */
    ArchiveGroup updateOwner(Long groupId, Long newOwnerUserId);

    void delete(Long id);
}
