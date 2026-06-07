package com.openquartz.easyarchive.core.source.mysql;

import com.openquartz.easyarchive.core.connection.entity.ArchiveConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MysqlSourceTest {

    @Test
    void closeShouldIgnoreUninitializedRunner() {
        MysqlSource source = new MysqlSource(new ArchiveConnection(), 1L, null, "select 1", false, null);

        assertDoesNotThrow(source::close);
    }
}
