package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByIdMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupItemByTimeMapper;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
import com.openquartz.easyarchive.starter.operationlog.OperationLogRecorder;
import com.openquartz.easyarchive.starter.operationlog.presenter.ArchiveGroupItemOperationLogPresenter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArchiveGroupItemByIdServiceImplTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveGroupItemByIdMapper idMapper = mock(ArchiveGroupItemByIdMapper.class);
    private final ArchiveGroupItemByTimeMapper timeMapper = mock(ArchiveGroupItemByTimeMapper.class);
    private final ArchiveGroupItemOperationLogPresenter archiveGroupItemOperationLogPresenter = mock(ArchiveGroupItemOperationLogPresenter.class);
    private final OperationLogRecorder operationLogRecorder = mock(OperationLogRecorder.class);
    private final ArchiveGroupItemByIdServiceImpl service = new ArchiveGroupItemByIdServiceImpl(groupMapper, idMapper, timeMapper,
            archiveGroupItemOperationLogPresenter, operationLogRecorder);

    @Test
    void shouldRejectPriorityConflictAcrossBothItemTables() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 5, null)).thenReturn(0);
        when(timeMapper.countPriority(10L, 5, null)).thenReturn(1);

        ArchiveGroupItemById item = validIdItem();
        item.setGroupId(10L);
        item.setPriority(5);

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(idMapper, never()).insert(any());
    }

    @Test
    void shouldCreateValidIdItemWithGroupIdAndDefaults() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 10, null)).thenReturn(0);
        when(timeMapper.countPriority(10L, 10, null)).thenReturn(0);

        ArchiveGroupItemById item = validIdItem();
        item.setGroupId(99L);
        item.setEnableStatus(null);
        item.setEnableClean(null);
        item.setEnableWrite(null);

        ArchiveGroupItemById created = service.create(10L, item);

        assertSame(item, created);
        assertEquals(10L, item.getGroupId());
        assertEquals(0, item.getEnableStatus());
        assertEquals(0, item.getEnableClean());
        assertEquals(0, item.getEnableWrite());
        verify(idMapper).insert(item);
    }

    @Test
    void shouldRejectMissingRequiredFields() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        ArchiveGroupItemById item = validIdItem();
        item.setSourceTable(" ");

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(idMapper, never()).insert(any());
    }

    @Test
    void shouldRejectInvalidIdSpecificRanges() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        ArchiveGroupItemById item = validIdItem();
        item.setStepRounds(0);

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(idMapper, never()).insert(any());
    }

    @Test
    void shouldRejectCleanWithoutWriteWhenEnabled() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        ArchiveGroupItemById item = validIdItem();
        item.setEnableStatus(0);
        item.setEnableClean(0);
        item.setEnableWrite(1);

        assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
        verify(idMapper, never()).insert(any());
    }

    @Test
    void shouldFindByGroupId() {
        List<ArchiveGroupItemById> items = Arrays.asList(validIdItem());
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectByGroupId(10L, 0)).thenReturn(items);

        assertSame(items, service.findByGroupId(10L, 0));
    }

    @Test
    void shouldFindExistingItemById() {
        ArchiveGroupItemById item = validIdItem();
        item.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectById(20L, 10L)).thenReturn(item);

        assertSame(item, service.findById(10L, 20L));
    }

    @Test
    void shouldRejectFindByIdWhenItemDoesNotExistUnderGroup() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectById(20L, 10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.findById(10L, 20L));
    }

    @Test
    void shouldUpdateExistingIdItemAndExcludeCurrentIdPriority() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 10, 20L)).thenReturn(0);
        when(timeMapper.countPriority(10L, 10, null)).thenReturn(0);

        ArchiveGroupItemById item = validIdItem();
        item.setGroupId(99L);

        ArchiveGroupItemById updated = service.update(10L, 20L, item);

        assertSame(item, updated);
        assertEquals(10L, item.getGroupId());
        assertEquals(20L, item.getId());
        verify(idMapper).update(item);
    }

    @Test
    void shouldAllowNullStatusOnUpdateForPartialUpdate() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        existing.setPauseMs(50);
        ArchiveGroupItemById item = validIdItem();
        item.setEnableStatus(null);
        item.setEnableClean(null);
        item.setEnableWrite(null);
        item.setPauseMs(null);
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 10, 20L)).thenReturn(0);
        when(timeMapper.countPriority(10L, 10, null)).thenReturn(0);

        ArchiveGroupItemById updated = service.update(10L, 20L, item);

        assertSame(item, updated);
        assertEquals(0, updated.getEnableStatus());
        assertEquals(0, updated.getEnableClean());
        assertEquals(0, updated.getEnableWrite());
        assertEquals(50, updated.getPauseMs());
        verify(idMapper).update(item);
    }

    @Test
    void shouldMergeRequiredFieldsFromExistingOnPartialUpdate() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        existing.setPauseMs(50);
        ArchiveGroupItemById item = new ArchiveGroupItemById();
        item.setPriority(10);
        item.setIdColumn(null);
        item.setStartId(null);
        item.setEndId(null);
        item.setStepCount(null);
        item.setStepRounds(null);
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.countPriority(10L, 10, 20L)).thenReturn(0);
        when(timeMapper.countPriority(10L, 10, null)).thenReturn(0);

        ArchiveGroupItemById updated = service.update(10L, 20L, item);

        assertSame(item, updated);
        assertEquals("t_order", updated.getSourceTable());
        assertEquals("t_order_archive", updated.getTargetTable());
        assertEquals("select id from t_order where id >= ? and id < ?", updated.getFetchSql());
        assertEquals("id", updated.getIdColumn());
        assertEquals("0", updated.getStartId());
        assertEquals("10000", updated.getEndId());
        assertEquals(1000, updated.getStepCount());
        assertEquals(5000, updated.getStepRounds());
        assertEquals(50, updated.getPauseMs());
        verify(idMapper).update(item);
    }

    @Test
    void shouldUpdateStatusForExistingItem() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);

        service.updateStatus(10L, 20L, 1);

        verify(idMapper).updateStatus(20L, 10L, 1);
        verify(operationLogRecorder).record(any());
    }

    @Test
    void shouldRejectInvalidStatusUpdate() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 20L, 2));
        verify(idMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shouldRejectEnableStatusUpdateWhenExistingItemIsUnsafe() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        existing.setEnableStatus(1);
        existing.setEnableClean(0);
        existing.setEnableWrite(1);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 20L, 0));
        verify(idMapper, never()).updateStatus(any(), any(), any());
    }

    @Test
    void shouldDeleteExistingItem() {
        ArchiveGroupItemById existing = validIdItem();
        existing.setId(20L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(idMapper.selectById(20L, 10L)).thenReturn(existing);

        service.delete(10L, 20L);

        verify(idMapper).deleteById(20L, 10L);
        verify(operationLogRecorder).record(any());
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

    private static ArchiveGroupItemById validIdItem() {
        ArchiveGroupItemById item = new ArchiveGroupItemById();
        item.setGroupId(10L);
        item.setSourceTable("t_order");
        item.setTargetTable("t_order_archive");
        item.setPriority(10);
        item.setFetchSql("select id from t_order where id >= ? and id < ?");
        item.setDeleteWhere("1 = 1");
        item.setStartId("0");
        item.setEndId("10000");
        item.setStepCount(1000);
        item.setStepRounds(5000);
        item.setEnableClean(0);
        item.setEnableWrite(0);
        item.setEnableStatus(0);
        item.setIdColumn("id");
        return item;
    }
}
