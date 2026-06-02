package com.openquartz.easyarchive.starter.rule;

import com.openquartz.easyarchive.core.rule.ArchiveRuleLoader;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Loads enabled archive items for one platform-managed archive group.
 */
public class PlatformArchiveRuleLoader implements ArchiveRuleLoader {

    private final Long groupId;
    private final ArchiveGroupItemByIdMapper idMapper;
    private final ArchiveGroupItemByTimeMapper timeMapper;

    public PlatformArchiveRuleLoader(Long groupId,
                                     ArchiveGroupItemByIdMapper idMapper,
                                     ArchiveGroupItemByTimeMapper timeMapper) {
        this.groupId = groupId;
        this.idMapper = idMapper;
        this.timeMapper = timeMapper;
    }

    @Override
    public List<ArchiveGroupItem> load() {
        List<ArchiveGroupItem> items = new ArrayList<>();
        items.addAll(idMapper.selectByGroupId(groupId, 0));
        items.addAll(timeMapper.selectByGroupId(groupId, 0));
        items.sort(Comparator.comparing(ArchiveGroupItem::getPriority));
        return items;
    }
}
