package com.openquartz.easyarchive.starter.rule;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformArchiveRuleLoaderTest {

    private final ArchiveGroupItemByIdMapper idMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final PlatformArchiveRuleLoader loader = new PlatformArchiveRuleLoader(10L, idMapper, timeMapper);

    @Test
    void shouldMergeEnabledItemsSortedByPriority() {
        ArchiveGroupItemById idItem = new ArchiveGroupItemById();
        idItem.setId(1L);
        idItem.setGroupId(10L);
        idItem.setPriority(20);
        idItem.setEnableStatus(0);

        ArchiveGroupItemByTime timeItem = new ArchiveGroupItemByTime();
        timeItem.setId(2L);
        timeItem.setGroupId(10L);
        timeItem.setPriority(10);
        timeItem.setEnableStatus(0);

        when(idMapper.selectByGroupId(10L, 0)).thenReturn(Collections.singletonList(idItem));
        when(timeMapper.selectByGroupId(10L, 0)).thenReturn(Collections.singletonList(timeItem));

        List<ArchiveGroupItem> items = loader.load();

        assertEquals(2, items.size());
        assertTrue(items.get(0) instanceof ArchiveGroupItemByTime);
        assertTrue(items.get(1) instanceof ArchiveGroupItemById);
    }
}
