package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;

import java.util.List;

/**
 * 归档分组服务
 */
public interface ArchiveGroupService {

    List<ArchiveGroup> findAll(Integer enableStatus);

    List<ArchiveGroup> tree();

    ArchiveGroup findById(Long id);

    ArchiveGroup create(ArchiveGroup group);

    ArchiveGroup update(ArchiveGroup group);

    void updateStatus(Long id, Integer enableStatus);

    void delete(Long id);
}
