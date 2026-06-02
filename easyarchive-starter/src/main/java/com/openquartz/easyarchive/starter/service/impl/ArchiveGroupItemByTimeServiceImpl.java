package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.service.ArchiveGroupItemByTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 按时间归档分组明细服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveGroupItemByTimeServiceImpl implements ArchiveGroupItemByTimeService {

    private final ArchiveGroupMapper groupMapper;
    private final ArchiveGroupItemByIdMapper idMapper;
    private final ArchiveGroupItemByTimeMapper timeMapper;

    @Override
    public List<ArchiveGroupItemByTime> findByGroupId(Long groupId, Integer enableStatus) {
        ensureGroupExists(groupId);
        return timeMapper.selectByGroupId(groupId, enableStatus);
    }

    @Override
    public ArchiveGroupItemByTime findById(Long groupId, Long itemId) {
        ensureGroupExists(groupId);
        return ensureItemExists(groupId, itemId);
    }

    @Override
    public ArchiveGroupItemByTime create(Long groupId, ArchiveGroupItemByTime item) {
        ensureGroupExists(groupId);
        if (item == null) {
            throw new IllegalArgumentException("按时间归档明细不能为空");
        }
        item.setGroupId(groupId);
        applyCreateDefaults(item);
        validateForSave(groupId, item, null, null);
        timeMapper.insert(item);
        return item;
    }

    @Override
    public ArchiveGroupItemByTime update(Long groupId, Long itemId, ArchiveGroupItemByTime item) {
        ensureGroupExists(groupId);
        ArchiveGroupItemByTime existing = ensureItemExists(groupId, itemId);
        if (item == null) {
            throw new IllegalArgumentException("按时间归档明细不能为空");
        }
        item.setId(itemId);
        item.setGroupId(groupId);
        mergeExisting(item, existing);
        validateForSave(groupId, item, itemId, existing);
        timeMapper.update(item);
        return item;
    }

    @Override
    public void updateStatus(Long groupId, Long itemId, Integer enableStatus) {
        ensureGroupExists(groupId);
        ArchiveGroupItemByTime existing = ensureItemExists(groupId, itemId);
        validateEnableStatus(enableStatus);
        if (enableStatus == 0 && valueOrExisting(existing.getEnableClean(), existing.getEnableClean()) == 0
                && valueOrExisting(existing.getEnableWrite(), existing.getEnableWrite()) == 1) {
            throw new IllegalArgumentException("启用归档明细时不能只清理源数据而不写入目标数据");
        }
        timeMapper.updateStatus(itemId, groupId, enableStatus);
    }

    @Override
    public void delete(Long groupId, Long itemId) {
        ensureGroupExists(groupId);
        ensureItemExists(groupId, itemId);
        timeMapper.deleteById(itemId, groupId);
    }

    private void validateForSave(Long groupId, ArchiveGroupItemByTime item, Long excludeId, ArchiveGroupItemByTime existing) {
        trimStrings(item);
        validateRequired(item);
        validatePriority(groupId, item.getPriority(), excludeId, false);
        validatePositive(item.getStepCount(), "步长必须大于0");
        validateNonNegative(item.getPauseMs(), "暂停时间不能小于0");
        validateEnableStatusForSave(item.getEnableStatus());
        validateFlag(item.getEnableClean(), "清理开关不合法");
        validateFlag(item.getEnableWrite(), "写入开关不合法");
        validateUnsafeCleanWithoutWrite(item, existing);
        if (item.getStartTime() == null) {
            throw new IllegalArgumentException("开始时间不能为空");
        }
        if (item.getKeepDay() == null || item.getKeepDay() < 0) {
            throw new IllegalArgumentException("保留天数不能小于0");
        }
        validatePositive(item.getStepMinutes(), "时间滚动步长必须大于0");
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

    private ArchiveGroupItemByTime ensureItemExists(Long groupId, Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("归档明细ID不能为空");
        }
        ArchiveGroupItemByTime item = timeMapper.selectById(itemId, groupId);
        if (item == null) {
            throw new IllegalArgumentException("按时间归档明细不存在");
        }
        return item;
    }

    private void applyCreateDefaults(ArchiveGroupItemByTime item) {
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

    private void mergeExisting(ArchiveGroupItemByTime item, ArchiveGroupItemByTime existing) {
        if (item.getSourceTable() == null) {
            item.setSourceTable(existing.getSourceTable());
        }
        if (item.getTargetTable() == null) {
            item.setTargetTable(existing.getTargetTable());
        }
        if (item.getPriority() == null) {
            item.setPriority(existing.getPriority());
        }
        if (item.getFetchSql() == null) {
            item.setFetchSql(existing.getFetchSql());
        }
        if (item.getDeleteWhere() == null) {
            item.setDeleteWhere(existing.getDeleteWhere());
        }
        if (item.getStartTime() == null) {
            item.setStartTime(existing.getStartTime());
        }
        if (item.getKeepDay() == null) {
            item.setKeepDay(existing.getKeepDay());
        }
        if (item.getStepMinutes() == null) {
            item.setStepMinutes(existing.getStepMinutes());
        }
        if (item.getStepCount() == null) {
            item.setStepCount(existing.getStepCount());
        }
        if (item.getPauseMs() == null) {
            item.setPauseMs(existing.getPauseMs());
        }
        if (item.getEnableClean() == null) {
            item.setEnableClean(existing.getEnableClean());
        }
        if (item.getEnableWrite() == null) {
            item.setEnableWrite(existing.getEnableWrite());
        }
        if (item.getEnableStatus() == null) {
            item.setEnableStatus(existing.getEnableStatus());
        }
        if (item.getIdColumn() == null) {
            item.setIdColumn(existing.getIdColumn());
        }
    }

    private void trimStrings(ArchiveGroupItemByTime item) {
        item.setSourceTable(trim(item.getSourceTable()));
        item.setTargetTable(trim(item.getTargetTable()));
        item.setFetchSql(trim(item.getFetchSql()));
        item.setIdColumn(trim(item.getIdColumn()));
        item.setDeleteWhere(trim(item.getDeleteWhere()));
    }

    private void validateRequired(ArchiveGroupItemByTime item) {
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

    private void validateUnsafeCleanWithoutWrite(ArchiveGroupItemByTime item, ArchiveGroupItemByTime existing) {
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
