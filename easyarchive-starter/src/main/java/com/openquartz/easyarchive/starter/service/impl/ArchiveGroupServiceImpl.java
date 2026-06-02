package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.service.ArchiveGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 归档分组服务实现
 */
@Service
@RequiredArgsConstructor
public class ArchiveGroupServiceImpl implements ArchiveGroupService {

    private final ArchiveGroupMapper groupMapper;

    @Override
    public List<ArchiveGroup> findAll(Integer enableStatus) {
        return groupMapper.selectList(enableStatus);
    }

    @Override
    public List<ArchiveGroup> tree() {
        return groupMapper.selectList(null);
    }

    @Override
    public ArchiveGroup findById(Long id) {
        return groupMapper.selectById(id);
    }

    @Override
    public ArchiveGroup create(ArchiveGroup group) {
        validateForSave(group, true);
        if (group.getEnableStatus() == null) {
            group.setEnableStatus(0);
        }
        groupMapper.insert(group);
        return group;
    }

    @Override
    public ArchiveGroup update(ArchiveGroup group) {
        if (group == null || group.getId() == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        ensureExists(group.getId());
        validateForSave(group, false);
        groupMapper.update(group);
        return group;
    }

    @Override
    public void updateStatus(Long id, Integer enableStatus) {
        ensureExists(id);
        if (enableStatus == null || (enableStatus != 0 && enableStatus != 1)) {
            throw new IllegalArgumentException("启用状态不合法");
        }
        groupMapper.updateStatus(id, enableStatus);
    }

    @Override
    public void delete(Long id) {
        ensureExists(id);
        groupMapper.deleteById(id);
    }

    private ArchiveGroup ensureExists(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("分组ID不能为空");
        }
        ArchiveGroup group = groupMapper.selectById(id);
        if (group == null) {
            throw new IllegalArgumentException("归档分组不存在");
        }
        return group;
    }

    private void validateForSave(ArchiveGroup group, boolean create) {
        if (group == null) {
            throw new IllegalArgumentException("归档分组不能为空");
        }
        if (group.getGroupCode() == null || group.getGroupCode().trim().isEmpty()) {
            throw new IllegalArgumentException("分组编码不能为空");
        }
        if (group.getGroupName() == null || group.getGroupName().trim().isEmpty()) {
            throw new IllegalArgumentException("分组名称不能为空");
        }
        if (group.getSourceDatasourceId() == null || group.getTargetDatasourceId() == null) {
            throw new IllegalArgumentException("源和目标数据源不能为空");
        }
        ArchiveGroup existing = groupMapper.selectByCode(group.getGroupCode());
        if (existing != null && (create || !existing.getId().equals(group.getId()))) {
            throw new IllegalArgumentException("分组编码已存在");
        }
    }
}
