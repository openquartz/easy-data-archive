ALTER TABLE `sys_user`
    ADD COLUMN `role_code` VARCHAR(32) NOT NULL DEFAULT 'USER' COMMENT '角色编码: ADMIN/USER' AFTER `email`;

CREATE TABLE IF NOT EXISTS `ea_user_datasource_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `datasource_id` BIGINT NOT NULL COMMENT '数据源ID',
    `permission_type` VARCHAR(32) NOT NULL DEFAULT 'READ' COMMENT '权限类型',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT DEFAULT NULL COMMENT '创建人ID',
    `updater_id` BIGINT DEFAULT NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_ds_perm` (`user_id`, `datasource_id`, `permission_type`, `deleted`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_datasource_id` (`datasource_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户数据源权限表';
