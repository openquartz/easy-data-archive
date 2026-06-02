package com.openquartz.easyarchive.starter.service;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;

import java.util.List;

/**
 * 按时间归档分组明细服务
 */
public interface ArchiveGroupItemByTimeService {

    List<ArchiveGroupItemByTime> findByGroupId(Long groupId, Integer enableStatus);

    ArchiveGroupItemByTime findById(Long groupId, Long itemId);

    ArchiveGroupItemByTime create(Long groupId, ArchiveGroupItemByTime item);

    ArchiveGroupItemByTime update(Long groupId, Long itemId, ArchiveGroupItemByTime item);

    void updateStatus(Long groupId, Long itemId, Integer enableStatus);

    void delete(Long groupId, Long itemId);
}
