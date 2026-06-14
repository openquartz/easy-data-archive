SET NAMES utf8mb4;
DELIMITER //

DROP PROCEDURE IF EXISTS `ea_archive_v9_add_in_app_notifications`//

CREATE PROCEDURE `ea_archive_v9_add_in_app_notifications`()
BEGIN
    CREATE TABLE IF NOT EXISTS `ea_archive_group_notification_user` (
        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
        `group_id` BIGINT NOT NULL COMMENT '归档分组ID',
        `user_id` BIGINT NOT NULL COMMENT '通知用户ID',
        `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
        KEY `idx_user_id` (`user_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档分组站内通知成员';

    CREATE TABLE IF NOT EXISTS `ea_in_app_notification` (
        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
        `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型',
        `biz_id` BIGINT NOT NULL COMMENT '业务主键',
        `category` VARCHAR(32) NOT NULL COMMENT '通知分类',
        `level` VARCHAR(16) NOT NULL COMMENT '通知级别',
        `group_id` BIGINT NULL COMMENT '归档分组ID快照',
        `group_name` VARCHAR(128) NULL COMMENT '归档分组名称快照',
        `task_id` BIGINT NULL COMMENT '任务ID快照',
        `task_status` VARCHAR(16) NULL COMMENT '任务状态快照',
        `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
        `content_summary` VARCHAR(500) NOT NULL COMMENT '通知摘要',
        `payload_json` TEXT NOT NULL COMMENT '通知完整载荷',
        `source_time` DATETIME NOT NULL COMMENT '业务事件发生时间',
        `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_biz_type_biz_id_status` (`biz_type`, `biz_id`, `task_status`),
        KEY `idx_group_created` (`group_id`, `created_time`),
        KEY `idx_task_id` (`task_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台站内通知主表';

    CREATE TABLE IF NOT EXISTS `ea_in_app_notification_recipient` (
        `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
        `notification_id` BIGINT NOT NULL COMMENT '通知主表ID',
        `recipient_user_id` BIGINT NOT NULL COMMENT '接收人用户ID',
        `read_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
        `read_time` DATETIME NULL COMMENT '已读时间',
        `delivery_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1-已投递',
        `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_notification_user` (`notification_id`, `recipient_user_id`),
        KEY `idx_user_read_created` (`recipient_user_id`, `read_status`, `created_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台站内通知收件箱';
END//

DELIMITER ;

CALL `ea_archive_v9_add_in_app_notifications`();
DROP PROCEDURE `ea_archive_v9_add_in_app_notifications`;
