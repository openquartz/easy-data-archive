-- V3: Replace generic archive rule tables with concrete archive group item tables.

DROP TABLE IF EXISTS `ea_archive_rule_condition`;
DROP TABLE IF EXISTS `ea_archive_rule`;

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
