-- V2: 新增归档执行任务表 + 补全日志表字段

CREATE TABLE IF NOT EXISTS `ea_archive_group_execute_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '归档分组 ID',
    `start_time` DATETIME NULL COMMENT '执行开始时间',
    `end_time` DATETIME NULL COMMENT '执行结束时间',
    `execute_status` INT NOT NULL DEFAULT 0 COMMENT '0-等待 1-运行中 2-成功 3-失败',
    `error_msg` VARCHAR(1000) NULL COMMENT '执行异常信息',
    `processed_records` BIGINT NOT NULL DEFAULT 0 COMMENT '已处理记录数',
    `processed_speed` DECIMAL(18,2) NULL COMMENT '处理速度(记录/秒)',
    `heartbeat_time` DATETIME NULL COMMENT '最新心跳时间',
    `finished_flag` BIGINT NOT NULL DEFAULT 0 COMMENT '0-未完成, 否则为id',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` VARCHAR(64) NULL COMMENT '创建人ID',
    `updater_id` VARCHAR(64) NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_execute_status` (`execute_status`),
    INDEX `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档执行任务表';

-- 补全 ea_archive_task_log 表缺少的 BaseEntity 字段
ALTER TABLE `ea_archive_task_log`
    ADD COLUMN `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER `created_time`,
    ADD COLUMN `creator_id` VARCHAR(64) NULL COMMENT '创建人ID' AFTER `updated_time`,
    ADD COLUMN `updater_id` VARCHAR(64) NULL COMMENT '更新人ID' AFTER `creator_id`,
    ADD COLUMN `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记' AFTER `updater_id`;
