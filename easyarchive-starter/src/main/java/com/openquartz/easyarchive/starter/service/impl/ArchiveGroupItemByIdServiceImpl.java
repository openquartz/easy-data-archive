package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.service.ArchiveGroupItemByIdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按ID归档分组明细服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveGroupItemByIdServiceImpl implements ArchiveGroupItemByIdService {

    private final ArchiveGroupMapper groupMapper;
    private final ArchiveGroupItemByIdMapper idMapper;
    private final ArchiveGroupItemByTimeMapper timeMapper;

    @Override
    public List<ArchiveGroupItemById> findByGroupId(Long groupId, Integer enableStatus) {
        ensureGroupExists(groupId);
        return idMapper.selectByGroupId(groupId, enableStatus);
    }

    @Override
    public ArchiveGroupItemById findById(Long groupId, Long itemId) {
        ensureGroupExists(groupId);
        return ensureItemExists(groupId, itemId);
    }

    @Override
    public ArchiveGroupItemById create(Long groupId, ArchiveGroupItemById item) {
        ensureGroupExists(groupId);
        if (item == null) {
            throw new IllegalArgumentException("按ID归档明细不能为空");
        }
        item.setGroupId(groupId);
        applyCreateDefaults(item);
        validateForSave(groupId, item, null, null);
        idMapper.insert(item);
        return item;
    }

    @Override
    public ArchiveGroupItemById update(Long groupId, Long itemId, ArchiveGroupItemById item) {
        ensureGroupExists(groupId);
        ArchiveGroupItemById existing = ensureItemExists(groupId, itemId);
        if (item == null) {
            throw new IllegalArgumentException("按ID归档明细不能为空");
        }
        item.setId(itemId);
        item.setGroupId(groupId);
        validateForSave(groupId, item, itemId, existing);
        idMapper.update(item);
        return item;
    }

    @Override
    public void updateStatus(Long groupId, Long itemId, Integer enableStatus) {
        ensureGroupExists(groupId);
        ArchiveGroupItemById existing = ensureItemExists(groupId, itemId);
        validateEnableStatus(enableStatus);
        if (enableStatus == 0 && valueOrExisting(existing.getEnableClean(), existing.getEnableClean()) == 0
                && valueOrExisting(existing.getEnableWrite(), existing.getEnableWrite()) == 1) {
            throw new IllegalArgumentException("启用归档明细时不能只清理源数据而不写入目标数据");
        }
        idMapper.updateStatus(itemId, groupId, enableStatus);
    }

    @Override
    public void delete(Long groupId, Long itemId) {
        ensureGroupExists(groupId);
        ensureItemExists(groupId, itemId);
        idMapper.deleteById(itemId, groupId);
    }

    private void validateForSave(Long groupId, ArchiveGroupItemById item, Long excludeId, ArchiveGroupItemById existing) {
        trimStrings(item);
        validateRequired(item);
        validatePriority(groupId, item.getPriority(), excludeId, true);
        validatePositive(item.getStepCount(), "步长必须大于0");
        validateNonNegative(item.getPauseMs(), "暂停时间不能小于0");
        validateEnableStatusForSave(item.getEnableStatus());
        validateFlag(item.getEnableClean(), "清理开关不合法");
        validateFlag(item.getEnableWrite(), "写入开关不合法");
        validateUnsafeCleanWithoutWrite(item, existing);
        if (isBlank(item.getStartId())) {
            throw new IllegalArgumentException("开始ID不能为空");
        }
        if (isBlank(item.getEndId())) {
            throw new IllegalArgumentException("结束ID不能为空");
        }
        validatePositive(item.getStepRounds(), "滚动步长必须大于0");
    }

    private void validatePriority(Long groupId, Integer priority, Long excludeId, boolean idItem) {
        if (priority == null) {
            throw new IllegalArgumentException("优先级不能为空");
        }
        int idCount = idMapper.countPriority(groupId, priority, idItem ? excludeId : null);
        int timeCount = timeMapper.countPriority(groupId, priority, idItem ? null : excludeId);
        if (idCount + timeCount > 0) {
            throw new IllegalArgumentException("同一分组内优先级不能重复");
        }
    }

    private ArchiveGroup ensureGroupExists(Long groupId) {
        if (groupId == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        ArchiveGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("归档分组不存在");
        }
        return group;
    }

    private ArchiveGroupItemById ensureItemExists(Long groupId, Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("归档明细ID不能为空");
        }
        ArchiveGroupItemById item = idMapper.selectById(itemId, groupId);
        if (item == null) {
            throw new IllegalArgumentException("按ID归档明细不存在");
        }
        return item;
    }

    private void applyCreateDefaults(ArchiveGroupItemById item) {
        if (item.getEnableStatus() == null) {
            item.setEnableStatus(0);
        }
        if (item.getEnableClean() == null) {
            item.setEnableClean(0);
        }
        if (item.getEnableWrite() == null) {
            item.setEnableWrite(0);
        }
    }

    private void trimStrings(ArchiveGroupItemById item) {
        item.setSourceTable(trim(item.getSourceTable()));
        item.setTargetTable(trim(item.getTargetTable()));
        item.setFetchSql(trim(item.getFetchSql()));
        item.setIdColumn(trim(item.getIdColumn()));
        item.setStartId(trim(item.getStartId()));
        item.setEndId(trim(item.getEndId()));
        item.setDeleteWhere(trim(item.getDeleteWhere()));
    }

    private void validateRequired(ArchiveGroupItemById item) {
        if (isBlank(item.getSourceTable())) {
            throw new IllegalArgumentException("来源表不能为空");
        }
        if (isBlank(item.getTargetTable())) {
            throw new IllegalArgumentException("目标表不能为空");
        }
        if (isBlank(item.getFetchSql())) {
            throw new IllegalArgumentException("查询SQL不能为空");
        }
        if (isBlank(item.getIdColumn())) {
            throw new IllegalArgumentException("ID字段不能为空");
        }
    }

    private void validateUnsafeCleanWithoutWrite(ArchiveGroupItemById item, ArchiveGroupItemById existing) {
        Integer enableStatus = valueOrExisting(item.getEnableStatus(), existing == null ? null : existing.getEnableStatus());
        Integer enableClean = valueOrExisting(item.getEnableClean(), existing == null ? null : existing.getEnableClean());
        Integer enableWrite = valueOrExisting(item.getEnableWrite(), existing == null ? null : existing.getEnableWrite());
        if (enableStatus != null && enableStatus == 0 && enableClean != null && enableClean == 0
                && enableWrite != null && enableWrite == 1) {
            throw new IllegalArgumentException("启用归档明细时不能只清理源数据而不写入目标数据");
        }
    }

    private void validateEnableStatus(Integer enableStatus) {
        if (enableStatus == null || (enableStatus != 0 && enableStatus != 1)) {
            throw new IllegalArgumentException("启用状态不合法");
        }
    }

    private void validateEnableStatusForSave(Integer enableStatus) {
        if (enableStatus != null) {
            validateEnableStatus(enableStatus);
        }
    }

    private void validateFlag(Integer value, String message) {
        if (value != null && value != 0 && value != 1) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validatePositive(Integer value, String message) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private void validateNonNegative(Integer value, String message) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private Integer valueOrExisting(Integer value, Integer existing) {
        return value == null ? existing : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
