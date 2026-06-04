ALTER TABLE `ea_archive_group`
    ADD COLUMN `notify_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '0-关闭通知 1-开启通知' AFTER `enable_status`,
    ADD COLUMN `notify_channel` VARCHAR(16) NULL COMMENT '通知渠道：FEISHU/WECOM' AFTER `notify_enabled`,
    ADD COLUMN `notify_webhook_url` VARCHAR(500) NULL COMMENT '通知 webhook 地址' AFTER `notify_channel`;
