-- Keep notification columns aligned for upgraded databases that were created
-- before the fields were folded back into V1.
DELIMITER //

DROP PROCEDURE IF EXISTS `ea_archive_v6_add_group_notification`//

CREATE PROCEDURE `ea_archive_v6_add_group_notification`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_group'
          AND column_name = 'notify_enabled'
    ) THEN
        ALTER TABLE `ea_archive_group`
            ADD COLUMN `notify_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '0-关闭通知 1-开启通知' AFTER `enable_status`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_group'
          AND column_name = 'notify_channel'
    ) THEN
        ALTER TABLE `ea_archive_group`
            ADD COLUMN `notify_channel` VARCHAR(16) NULL COMMENT '通知渠道：FEISHU/WECOM' AFTER `notify_enabled`;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_group'
          AND column_name = 'notify_webhook_url'
    ) THEN
        ALTER TABLE `ea_archive_group`
            ADD COLUMN `notify_webhook_url` VARCHAR(500) NULL COMMENT '通知 webhook 地址' AFTER `notify_channel`;
    END IF;
END//

DELIMITER ;

CALL `ea_archive_v6_add_group_notification`();
DROP PROCEDURE `ea_archive_v6_add_group_notification`;
