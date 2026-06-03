ALTER TABLE `sys_operation_log`
    ADD COLUMN `button_name` VARCHAR(128) NULL COMMENT '按钮名称' AFTER `action_code`,
    ADD COLUMN `biz_type` VARCHAR(64) NULL COMMENT '业务对象类型' AFTER `button_name`,
    ADD COLUMN `biz_id` BIGINT NULL COMMENT '业务对象主键' AFTER `biz_type`,
    ADD COLUMN `biz_key` VARCHAR(255) NULL COMMENT '业务对象摘要' AFTER `biz_id`,
    ADD COLUMN `content` TEXT NULL COMMENT '中文操作内容' AFTER `biz_key`,
    ADD COLUMN `error_message` VARCHAR(500) NULL COMMENT '失败原因' AFTER `content`;

ALTER TABLE `sys_operation_log`
    ADD INDEX `idx_biz_type_id` (`biz_type`, `biz_id`),
    ADD INDEX `idx_module_action_time` (`module_code`, `action_code`, `operate_time`);
