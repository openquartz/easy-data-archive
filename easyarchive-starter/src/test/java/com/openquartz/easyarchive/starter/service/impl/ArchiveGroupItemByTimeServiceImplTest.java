package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveGroupItemByTimeServiceImplTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveGroupItemByIdMapper idMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final ArchiveGroupItemByTimeServiceImpl service = new ArchiveGroupItemByTimeServiceImpl(groupMapper, idMapper, timeMapper);

    @Test
    void shouldRejectCleanWithoutWriteWhenEnabled() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        ArchiveGroupItemByTime item = validTimeItem();
        item.setEnableStatus(0);
        item.setEnableClean(0);
        item.setEnableWrite(1);

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(timeMapper, never()).insert(any());
    }

    @Test
    void shouldRejectPriorityConflictAcrossBothItemTables() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 20, null)).thenReturn(1);
        when(timeMapper.countPriority(10L, 20, null)).thenReturn(0);

        ArchiveGroupItemByTime item = validTimeItem();

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(timeMapper, never()).insert(any());
    }

    @Test
    void shouldCreateValidTimeItemWithGroupIdAndDefaults() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 20, null)).thenReturn(0);
        when(timeMapper.countPriority(10L, 20, null)).thenReturn(0);

        ArchiveGroupItemByTime item = validTimeItem();
        item.setGroupId(99L);
        item.setEnableStatus(null);
        item.setEnableClean(null);
        item.setEnableWrite(null);

        ArchiveGroupItemByTime created = service.create(10L, item);

        assertSame(item, created);
        assertEquals(10L, item.getGroupId());
        assertEquals(0, item.getEnableStatus());
        assertEquals(0, item.getEnableClean());
        assertEquals(0, item.getEnableWrite());
        verify(timeMapper).insert(item);
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        ArchiveGroupItemByTime item = validTimeItem();
        item.setFetchSql("");

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(timeMapper, never()).insert(any());
    }

    @Test
    void shouldRejectInvalidTimeSpecificRanges() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        ArchiveGroupItemByTime item = validTimeItem();
        item.setStepMinutes(0);

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(timeMapper, never()).insert(any());
    }

    @Test
    void shouldFindByGroupId() {
        List<ArchiveGroupItemByTime> items = Arrays.asList(validTimeItem());
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectByGroupId(10L, 0)).thenReturn(items);

        assertSame(items, service.findByGroupId(10L, 0));
    }

    @Test
    void shouldFindExistingItemById() {
        ArchiveGroupItemByTime item = validTimeItem();
        item.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectById(20L, 10L)).thenReturn(item);

        assertSame(item, service.findById(10L, 20L));
    }

    @Test
    void shouldRejectFindByIdWhenItemDoesNotExistUnderGroup() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectById(20L, 10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.findById(10L, 20L));
    }

    @Test
    void shouldUpdateExistingTimeItemAndExcludeCurrentIdPriority() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 20, null)).thenReturn(0);
        when(timeMapper.countPriority(10L, 20, 20L)).thenReturn(0);

        ArchiveGroupItemByTime item = validTimeItem();
        item.setGroupId(99L);

        ArchiveGroupItemByTime updated = service.update(10L, 20L, item);

        assertSame(item, updated);
        assertEquals(10L, item.getGroupId());
        assertEquals(20L, item.getId());
        verify(timeMapper).update(item);
    }

    @Test
    void shouldAllowNullStatusOnUpdateForPartialUpdate() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        existing.setPauseMs(50);
        ArchiveGroupItemByTime item = validTimeItem();
        item.setEnableStatus(null);
        item.setEnableClean(null);
        item.setEnableWrite(null);
        item.setPauseMs(null);
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 20, null)).thenReturn(0);
        when(timeMapper.countPriority(10L, 20, 20L)).thenReturn(0);

        ArchiveGroupItemByTime updated = service.update(10L, 20L, item);

        assertSame(item, updated);
        assertEquals(0, updated.getEnableStatus());
        assertEquals(0, updated.getEnableClean());
        assertEquals(0, updated.getEnableWrite());
        assertEquals(50, updated.getPauseMs());
        verify(timeMapper).update(item);
    }

    @Test
    void shouldMergeRequiredFieldsFromExistingOnPartialUpdate() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        existing.setPauseMs(50);
        ArchiveGroupItemByTime item = new ArchiveGroupItemByTime();
        item.setPriority(20);
        item.setIdColumn(null);
        item.setStepCount(null);
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 20, null)).thenReturn(0);
        when(timeMapper.countPriority(10L, 20, 20L)).thenReturn(0);

        ArchiveGroupItemByTime updated = service.update(10L, 20L, item);

        assertSame(item, updated);
        assertEquals("t_order", updated.getSourceTable());
        assertEquals("t_order_archive", updated.getTargetTable());
        assertEquals("select id from t_order where created_time >= ? and created_time < ?", updated.getFetchSql());
        assertEquals("id", updated.getIdColumn());
        assertEquals(new Date(0L), updated.getStartTime());
        assertEquals(30, updated.getKeepDay());
        assertEquals(60, updated.getStepMinutes());
        assertEquals(1000, updated.getStepCount());
        assertEquals(50, updated.getPauseMs());
        verify(timeMapper).update(item);
    }

    @Test
    void shouldUpdateStatusForExistingItem() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);

        service.updateStatus(10L, 20L, 1);

        verify(timeMapper).updateStatus(20L, 10L, 1);
    }

    @Test
    void shouldRejectInvalidStatusUpdate() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 20L, 2));
        verify(timeMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shouldRejectEnableStatusUpdateWhenExistingItemIsUnsafe() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        existing.setEnableStatus(1);
        existing.setEnableClean(0);
        existing.setEnableWrite(1);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 20L, 0));
        verify(timeMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shouldDeleteExistingItem() {
        ArchiveGroupItemByTime existing = validTimeItem();
        existing.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(timeMapper.selectById(20L, 10L)).thenReturn(existing);

        service.delete(10L, 20L);

        verify(timeMapper).deleteById(20L, 10L);
    }

    private static ArchiveGroup enabledGroup() {
        ArchiveGroup group = new ArchiveGroup();
        group.setId(10L);
        group.setGroupCode("ORDER_ARCHIVE");
        group.setGroupName("Order Archive");
        group.setSourceDatasourceId(1L);
        group.setTargetDatasourceId(2L);
        group.setEnableStatus(0);
        return group;
    }

    private static ArchiveGroupItemByTime validTimeItem() {
        ArchiveGroupItemByTime item = new ArchiveGroupItemByTime();
        item.setGroupId(10L);
        item.setSourceTable("t_order");
        item.setTargetTable("t_order_archive");
        item.setPriority(20);
        item.setFetchSql("select id from t_order where created_time >= ? and created_time < ?");
        item.setDeleteWhere("1 = 1");
        item.setStartTime(new Date(0L));
        item.setKeepDay(30);
        item.setStepMinutes(60);
        item.setStepCount(1000);
        item.setEnableClean(0);
        item.setEnableWrite(0);
        item.setEnableStatus(0);
        item.setIdColumn("id");
        return item;
    }
}
