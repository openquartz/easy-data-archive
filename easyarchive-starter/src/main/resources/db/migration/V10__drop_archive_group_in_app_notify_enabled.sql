SET NAMES utf8mb4;
DELIMITER //

DROP PROCEDURE IF EXISTS `ea_archive_v10_drop_archive_group_in_app_notify_enabled`//

CREATE PROCEDURE `ea_archive_v10_drop_archive_group_in_app_notify_enabled`()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'ea_archive_group'
          AND column_name = 'in_app_notify_enabled'
    ) THEN
        ALTER TABLE `ea_archive_group`
            DROP COLUMN `in_app_notify_enabled`;
    END IF;
END//

DELIMITER ;

CALL `ea_archive_v10_drop_archive_group_in_app_notify_enabled`();
DROP PROCEDURE `ea_archive_v10_drop_archive_group_in_app_notify_enabled`;
