-- V3: Replace generic archive rule tables with concrete archive group item tables.

-- MySQL 5.7 does not support RENAME TABLE IF EXISTS or ADD COLUMN IF NOT EXISTS.
-- Use a short migration procedure to preserve old generic rule data when present
-- and keep this migration safe for databases that never had the generic tables.
DELIMITER //

DROP PROCEDURE IF EXISTS `ea_archive_v3_migrate`//

CREATE PROCEDURE `ea_archive_v3_migrate`()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_rule_condition'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_rule_condition_backup_v3'
    ) THEN
        RENAME TABLE `ea_archive_rule_condition` TO `ea_archive_rule_condition_backup_v3`;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_rule'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_rule_backup_v3'
    ) THEN
        RENAME TABLE `ea_archive_rule` TO `ea_archive_rule_backup_v3`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_task'
          AND column_name = 'current_item_type'
    ) THEN
        ALTER TABLE `ea_archive_task`
            ADD COLUMN `current_item_type` VARCHAR(16) NULL COMMENT '当前明细类型 ID/TIME' AFTER `current_rule_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_task_detail'
          AND column_name = 'item_type'
    ) THEN
        ALTER TABLE `ea_archive_task_detail`
            ADD COLUMN `item_type` VARCHAR(16) NOT NULL DEFAULT 'ID' COMMENT 'ID/TIME' AFTER `rule_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_task_progress'
          AND column_name = 'current_item_type'
    ) THEN
        ALTER TABLE `ea_archive_task_progress`
            ADD COLUMN `current_item_type` VARCHAR(16) NULL COMMENT '当前明细类型 ID/TIME' AFTER `current_rule_id`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_task_log'
          AND column_name = 'item_type'
    ) THEN
        ALTER TABLE `ea_archive_task_log`
            ADD COLUMN `item_type` VARCHAR(16) NULL COMMENT '明细类型 ID/TIME' AFTER `rule_id`;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_rule_backup_v3'
    ) THEN
        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'ea_archive_task_detail'
              AND column_name = 'item_type'
        ) THEN
            UPDATE `ea_archive_task_detail` task_detail
            INNER JOIN `ea_archive_rule_backup_v3` rule_backup
                ON task_detail.`rule_id` = rule_backup.`id`
            SET task_detail.`item_type` = rule_backup.`rule_type`;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'ea_archive_task_log'
              AND column_name = 'item_type'
        ) THEN
            UPDATE `ea_archive_task_log` task_log
            INNER JOIN `ea_archive_rule_backup_v3` rule_backup
                ON task_log.`rule_id` = rule_backup.`id`
            SET task_log.`item_type` = rule_backup.`rule_type`
            WHERE task_log.`rule_id` IS NOT NULL;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'ea_archive_task'
              AND column_name = 'current_item_type'
        ) THEN
            UPDATE `ea_archive_task` task
            INNER JOIN `ea_archive_rule_backup_v3` rule_backup
                ON task.`current_rule_id` = rule_backup.`id`
            SET task.`current_item_type` = rule_backup.`rule_type`
            WHERE task.`current_rule_id` IS NOT NULL;
        END IF;

        IF EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'ea_archive_task_progress'
              AND column_name = 'current_item_type'
        ) THEN
            UPDATE `ea_archive_task_progress` progress
            INNER JOIN `ea_archive_rule_backup_v3` rule_backup
                ON progress.`current_rule_id` = rule_backup.`id`
            SET progress.`current_item_type` = rule_backup.`rule_type`
            WHERE progress.`current_rule_id` IS NOT NULL;
        END IF;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_task_detail'
          AND column_name = 'item_type'
    ) THEN
        ALTER TABLE `ea_archive_task_detail`
            MODIFY COLUMN `item_type` VARCHAR(16) NOT NULL COMMENT 'ID/TIME';
    END IF;
END//

DELIMITER ;

CALL `ea_archive_v3_migrate`();
DROP PROCEDURE `ea_archive_v3_migrate`;

CREATE TABLE IF NOT EXISTS `ea_archive_group_item_by_id` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '归档分组 ID',
    `source_table` VARCHAR(128) NOT NULL COMMENT '来源表',
    `target_table` VARCHAR(128) NOT NULL COMMENT '目标表',
    `priority` INT NOT NULL COMMENT '组内执行优先级',
    `fetch_sql` TEXT NOT NULL COMMENT '抓取 SQL',
    `delete_where` TEXT NULL COMMENT '删除保护条件',
    `start_id` VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '起始 ID 表达式',
    `end_id` VARCHAR(255) NOT NULL DEFAULT '9223372036854775807' COMMENT '结束 ID 表达式',
    `step_count` INT NOT NULL DEFAULT 1000 COMMENT '单批大小',
    `step_rounds` INT NOT NULL DEFAULT 5000 COMMENT 'ID 滚动窗口',
    `pause_ms` INT NULL COMMENT '批间停顿毫秒',
    `enable_clean` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用清理 1-不清理',
    `enable_write` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用写入 1-不写入',
    `enable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `id_column` VARCHAR(64) NOT NULL DEFAULT 'ID' COMMENT 'ID 字段名',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` VARCHAR(64) NULL COMMENT '创建人ID',
    `updater_id` VARCHAR(64) NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_status` (`group_id`, `enable_status`),
    INDEX `idx_group_priority_id` (`group_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按 ID 归档分组明细';

CREATE TABLE IF NOT EXISTS `ea_archive_group_item_by_time` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '归档分组 ID',
    `source_table` VARCHAR(128) NOT NULL COMMENT '来源表',
    `target_table` VARCHAR(128) NOT NULL COMMENT '目标表',
    `priority` INT NOT NULL COMMENT '组内执行优先级',
    `fetch_sql` TEXT NOT NULL COMMENT '抓取 SQL',
    `delete_where` TEXT NULL COMMENT '删除保护条件',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `keep_day` INT NOT NULL COMMENT '保留天数',
    `step_minutes` INT NOT NULL COMMENT '时间滚动窗口分钟',
    `step_count` INT NOT NULL DEFAULT 1000 COMMENT '单批大小',
    `pause_ms` INT NULL COMMENT '批间停顿毫秒',
    `enable_clean` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用清理 1-不清理',
    `enable_write` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用写入 1-不写入',
    `enable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `id_column` VARCHAR(64) NOT NULL DEFAULT 'ID' COMMENT 'ID 字段名',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` VARCHAR(64) NULL COMMENT '创建人ID',
    `updater_id` VARCHAR(64) NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_status` (`group_id`, `enable_status`),
    INDEX `idx_group_priority_time` (`group_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按时间归档分组明细';
