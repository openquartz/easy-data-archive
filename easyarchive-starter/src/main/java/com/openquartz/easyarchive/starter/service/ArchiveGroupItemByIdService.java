package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;

import java.util.List;

/**
 * 按ID归档分组明细服务
 */
public interface ArchiveGroupItemByIdService {

    List<ArchiveGroupItemById> findByGroupId(Long groupId, Integer enableStatus);

    ArchiveGroupItemById findById(Long groupId, Long itemId);

    ArchiveGroupItemById create(Long groupId, ArchiveGroupItemById item);

    ArchiveGroupItemById update(Long groupId, Long itemId, ArchiveGroupItemById item);

    void updateStatus(Long groupId, Long itemId, Integer enableStatus);

    void delete(Long groupId, Long itemId);
}
