package com.openquartz.easyarchive.core.source.mysql;

import com.openquartz.easyarchive.common.api.model.DataIterator;
import com.openquartz.easyarchive.common.api.model.TableInfo;
import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import org.junit.jupiter.api.Test;
import org.apache.ibatis.jdbc.SqlRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MysqlSourceTest {

    @Test
    void closeShouldIgnoreUninitializedRunner() {
        MysqlSource source = new MysqlSource(new ArchiveConnection(), 1L, null, "select 1", false, null);

        assertDoesNotThrow(source::close);
    }

    @Test
    void readShouldUseConfiguredIdColumnForOrderingAndIdResolution() throws Exception {
        MysqlSource source = new MysqlSource(
            new ArchiveConnection(),
            1L,
            TableInfo.of("archive_order", "custom_id"),
            "select custom_id from archive_order where created_time >= ? and created_time < ?",
            false,
            null
        );
        SqlRunner runner = mock(SqlRunner.class);
        setField(source, "runner", runner);
        when(runner.selectAll(any(String.class), any(), any()))
            .thenReturn(List.of(Map.of("custom_id", 11L)));

        DataIterator iterator = source.read("2026-06-01 00:00:00", "2026-06-01 00:30:00", 1, 100, 10);

        assertTrue(iterator.hasNext());
        verify(runner).selectAll(
            eq("select custom_id from archive_order where created_time >= ? and created_time < ? order by custom_id limit 100,100"),
            any(),
            any()
        );
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
