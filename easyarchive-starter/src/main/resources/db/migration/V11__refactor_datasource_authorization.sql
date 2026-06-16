SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ea_archive_connection_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `datasource_id` BIGINT NOT NULL,
    `permission_level` VARCHAR(32) NOT NULL,
    `grant_source` VARCHAR(32) NOT NULL,
    `granted_by_user_id` BIGINT NOT NULL,
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_ds_level` (`user_id`, `datasource_id`, `permission_level`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE `sys_user`
SET `role_code` = 'platform_admin'
WHERE UPPER(`role_code`) IN ('PLATFORM_ADMIN', 'ADMIN');

UPDATE `sys_user`
SET `role_code` = 'archive_admin'
WHERE UPPER(`role_code`) IN ('ARCHIVE_ADMIN', 'USER');

UPDATE `sys_user`
SET `role_code` = 'normal_user'
WHERE UPPER(`role_code`) IN ('AUDITOR', 'OBSERVER');

INSERT INTO `ea_archive_connection_permission`
(`user_id`, `datasource_id`, `permission_level`, `grant_source`, `granted_by_user_id`, `created_time`, `updated_time`, `deleted`)
SELECT p.`user_id`,
       p.`datasource_id`,
       CASE
           WHEN LOWER(u.`role_code`) = 'archive_admin' THEN 'MANAGE'
           ELSE 'USE'
       END,
       'SYSTEM_ASSIGN',
       CASE
           WHEN p.`creator_id` REGEXP '^[0-9]+$' THEN CAST(p.`creator_id` AS UNSIGNED)
           ELSE 1
       END,
       p.`created_time`,
       p.`updated_time`,
       0
FROM `ea_user_datasource_permission` p
JOIN `sys_user` u ON u.`id` = p.`user_id`
WHERE p.`deleted` = 0
  AND NOT EXISTS (
      SELECT 1
      FROM `ea_archive_connection_permission` ap
      WHERE ap.`user_id` = p.`user_id`
        AND ap.`datasource_id` = p.`datasource_id`
        AND ap.`permission_level` = CASE
            WHEN LOWER(u.`role_code`) = 'archive_admin' THEN 'MANAGE'
            ELSE 'USE'
        END
        AND ap.`deleted` = 0
  );

DROP TABLE IF EXISTS `ea_user_datasource_permission`;
