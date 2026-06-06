package com.openquartz.easyarchive.starter.db;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationLogSchemaSqlSyncTest {

    @Test
    void initSqlShouldContainExtendedOperationLogColumns() throws IOException {
        String sql = new String(
                new ClassPathResource("db/migration/V1__init_archive_platform.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        assertAll(
                () -> assertTrue(sql.contains("`button_name` VARCHAR(128)"), "missing button_name column"),
                () -> assertTrue(sql.contains("`biz_type` VARCHAR(64)"), "missing biz_type column"),
                () -> assertTrue(sql.contains("`biz_id` BIGINT"), "missing biz_id column"),
                () -> assertTrue(sql.contains("`biz_key` VARCHAR(255)"), "missing biz_key column"),
                () -> assertTrue(sql.contains("`content` TEXT"), "missing content column"),
                () -> assertTrue(sql.contains("`error_message` VARCHAR(500)"), "missing error_message column"),
                () -> assertTrue(sql.contains("INDEX `idx_biz_type_id` (`biz_type`, `biz_id`)"),
                        "missing idx_biz_type_id index"),
                () -> assertTrue(sql.contains("INDEX `idx_module_action_time` (`module_code`, `action_code`, `operate_time`)"),
                        "missing idx_module_action_time index")
        );
    }
}
