package com.openquartz.easyarchive.starter.service.impl;

import com.openquartz.easyarchive.core.rule.entity.ArchiveGroup;
import com.openquartz.easyarchive.starter.mapper.ArchiveGroupMapper;
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

class ArchiveGroupServiceImplTest {

    private final ArchiveGroupMapper groupMapper = mock(ArchiveGroupMapper.class);
    private final ArchiveGroupServiceImpl service = new ArchiveGroupServiceImpl(groupMapper);

    @Test
    void shouldRejectDuplicateGroupCodeOnCreate() {
        ArchiveGroup existing = new ArchiveGroup();
        existing.setId(1L);
        existing.setGroupCode("ORDER_ARCHIVE");
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = new ArchiveGroup();
        input.setGroupCode("ORDER_ARCHIVE");
        input.setGroupName("Order Archive");
        input.setSourceDatasourceId(1L);
        input.setTargetDatasourceId(2L);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectBlankGroupCodeOnCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupCode("   ");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectBlankGroupNameOnCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupName("");

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldRejectMissingDatasourceOnCreate() {
        ArchiveGroup missingSource = enabledGroup();
        missingSource.setId(null);
        missingSource.setSourceDatasourceId(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(missingSource));
        verify(groupMapper, never()).insert(any());

        ArchiveGroup missingTarget = enabledGroup();
        missingTarget.setId(null);
        missingTarget.setTargetDatasourceId(null);

        assertThrows(IllegalArgumentException.class, () -> service.create(missingTarget));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldCreateValidGroupWithDefaultStatus() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setEnableStatus(null);

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals(0, input.getEnableStatus());
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldRejectInvalidStatusOnCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setEnableStatus(2);

        assertThrows(IllegalArgumentException.class, () -> service.create(input));
        verify(groupMapper, never()).insert(any());
    }

    @Test
    void shouldTrimGroupCodeAndNameBeforeCreate() {
        ArchiveGroup input = enabledGroup();
        input.setId(null);
        input.setGroupCode(" ORDER_ARCHIVE ");
        input.setGroupName(" Order Archive ");

        ArchiveGroup created = service.create(input);

        assertSame(input, created);
        assertEquals("ORDER_ARCHIVE", input.getGroupCode());
        assertEquals("Order Archive", input.getGroupName());
        verify(groupMapper).selectByCode("ORDER_ARCHIVE");
        verify(groupMapper).insert(input);
    }

    @Test
    void shouldRejectDuplicateGroupCodeOnUpdate() {
        ArchiveGroup existing = enabledGroup();
        existing.setId(99L);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup input = enabledGroup();

        assertThrows(IllegalArgumentException.class, () -> service.update(input));
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldRejectUpdateWhenGroupDoesNotExist() {
        ArchiveGroup input = enabledGroup();
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.update(input));
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldRejectInvalidStatusOnUpdate() {
        ArchiveGroup input = enabledGroup();
        input.setEnableStatus(-1);
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        assertThrows(IllegalArgumentException.class, () -> service.update(input));
        verify(groupMapper, never()).update(any());
    }

    @Test
    void shouldAllowNullStatusOnUpdateForPartialUpdate() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setEnableStatus(null);
        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        verify(groupMapper).update(input);
    }

    @Test
    void shouldTrimGroupCodeAndNameBeforeUpdate() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setGroupCode(" ORDER_ARCHIVE ");
        input.setGroupName(" Order Archive ");
        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        assertEquals("ORDER_ARCHIVE", input.getGroupCode());
        assertEquals("Order Archive", input.getGroupName());
        verify(groupMapper).selectByCode("ORDER_ARCHIVE");
        verify(groupMapper).update(input);
    }

    @Test
    void shouldUpdateExistingGroupWhenCodeBelongsToSameGroup() {
        ArchiveGroup existing = enabledGroup();
        ArchiveGroup input = enabledGroup();
        input.setGroupName("Order Archive Updated");

        when(groupMapper.selectById(10L)).thenReturn(existing);
        when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

        ArchiveGroup updated = service.update(input);

        assertSame(input, updated);
        verify(groupMapper).update(input);
    }

    @Test
    void shouldRejectInvalidStatusUpdate() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 2));
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    @Test
    void shouldRejectStatusUpdateWhenGroupDoesNotExist() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.updateStatus(10L, 0));
        verify(groupMapper, never()).updateStatus(any(), any());
    }

    @Test
    void shouldDeleteExistingGroup() {
        when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

        service.delete(10L);

        verify(groupMapper).deleteById(10L);
    }

    @Test
    void shouldRejectDeleteWhenGroupDoesNotExist() {
        when(groupMapper.selectById(10L)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> service.delete(10L));
        verify(groupMapper, never()).deleteById(any());
    }

    @Test
    void shouldReturnFlatListForTree() {
        List<ArchiveGroup> groups = Arrays.asList(enabledGroup());
        when(groupMapper.selectList(null)).thenReturn(groups);

        assertSame(groups, service.tree());
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
}
