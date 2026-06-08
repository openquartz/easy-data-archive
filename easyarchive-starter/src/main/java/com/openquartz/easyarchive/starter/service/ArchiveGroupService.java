package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupOverviewView;
import com.openquartz.easyarchive.starter.model.dto.ArchiveGroupView;
import com.openquartz.easyarchive.starter.model.dto.PageResult;

import java.util.List;

/**
 * 归档分组服务
 */
public interface ArchiveGroupService {

    List<ArchiveGroupView> findAll(Integer enableStatus);

    /**
     * 分页查询归档分组（带权限过滤）
     *
     * @param enableStatus 启用状态
     * @param page 页码（从1开始）
     * @param size 每页大小
     * @return 分页结果
     */
    PageResult<ArchiveGroupView> findPage(Integer enableStatus, int page, int size);

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
