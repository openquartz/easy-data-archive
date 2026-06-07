-- EasyArchive Platform 数据库初始化脚本
-- 版本: 1.0.0
-- 创建时间: 2026-05-24
SET NAMES utf8mb4;

-- 权限与审计模块
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(64) NOT NULL COMMENT '登录账号，唯一',
    `password` VARCHAR(128) NOT NULL COMMENT 'BCrypt 密文',
    `real_name` VARCHAR(64) NOT NULL COMMENT '姓名',
    `mobile` VARCHAR(32) NULL COMMENT '手机',
    `email` VARCHAR(128) NULL COMMENT '邮箱',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `last_login_time` DATETIME NULL COMMENT '最近登录时间',
    `remark` VARCHAR(255) NULL COMMENT '备注',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `updater_id` BIGINT NULL COMMENT '更新人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_username` (`username`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码，唯一',
    `role_name` VARCHAR(64) NOT NULL COMMENT '角色名称',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `data_scope_type` VARCHAR(32) NULL COMMENT '数据范围类型',
    `remark` VARCHAR(255) NULL COMMENT '备注',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `updater_id` BIGINT NULL COMMENT '更新人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_code` (`role_code`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `permission_code` VARCHAR(128) NOT NULL COMMENT '权限码，唯一',
    `permission_name` VARCHAR(64) NOT NULL COMMENT '权限名',
    `permission_type` VARCHAR(16) NOT NULL COMMENT 'MENU/BUTTON/API',
    `parent_id` BIGINT NULL COMMENT '父节点',
    `route_path` VARCHAR(128) NULL COMMENT '前端路由',
    `component` VARCHAR(128) NULL COMMENT '前端组件',
    `sort_no` INT NOT NULL DEFAULT 0 COMMENT '排序',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '启用状态',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `updater_id` BIGINT NULL COMMENT '更新人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_permission_code` (`permission_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `role_id` BIGINT NOT NULL COMMENT '角色 ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_role` (`user_id`, `role_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `role_id` BIGINT NOT NULL COMMENT '角色 ID',
    `permission_id` BIGINT NOT NULL COMMENT '权限 ID',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_role_permission` (`role_id`, `permission_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

CREATE TABLE IF NOT EXISTS `sys_login_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `token_id` VARCHAR(64) NOT NULL COMMENT '会话标识',
    `login_ip` VARCHAR(64) NULL COMMENT '登录 IP',
    `user_agent` VARCHAR(255) NULL COMMENT '终端信息',
    `login_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-成功 1-登出 2-失效',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `logout_time` DATETIME NULL COMMENT '登出时间',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_token_id` (`token_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status_expire` (`login_status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录会话表';

CREATE TABLE IF NOT EXISTS `sys_operation_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '操作人',
    `module_code` VARCHAR(64) NOT NULL COMMENT '模块编码',
    `action_code` VARCHAR(64) NOT NULL COMMENT '操作类型',
    `button_name` VARCHAR(128) NULL COMMENT '按钮名称',
    `biz_type` VARCHAR(64) NULL COMMENT '业务对象类型',
    `biz_id` BIGINT NULL COMMENT '业务对象主键',
    `biz_key` VARCHAR(255) NULL COMMENT '业务对象摘要',
    `content` TEXT NULL COMMENT '中文操作内容',
    `request_uri` VARCHAR(255) NULL COMMENT '请求路径',
    `request_method` VARCHAR(16) NULL COMMENT 'HTTP 方法',
    `request_param` TEXT NULL COMMENT '参数摘要',
    `response_code` VARCHAR(32) NULL COMMENT '响应码',
    `result_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-成功 1-失败',
    `cost_ms` BIGINT NULL COMMENT '耗时',
    `client_ip` VARCHAR(64) NULL COMMENT 'IP',
    `error_message` VARCHAR(500) NULL COMMENT '失败原因',
    `operate_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_module_action` (`module_code`, `action_code`),
    INDEX `idx_biz_type_id` (`biz_type`, `biz_id`),
    INDEX `idx_module_action_time` (`module_code`, `action_code`, `operate_time`),
    INDEX `idx_result_status` (`result_status`),
    INDEX `idx_operate_time` (`operate_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 归档核心模块
CREATE TABLE IF NOT EXISTS `ea_archive_datasource` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `datasource_code` VARCHAR(64) NOT NULL COMMENT '编码，唯一',
    `datasource_name` VARCHAR(64) NOT NULL COMMENT '名称',
    `datasource_type` VARCHAR(32) NOT NULL COMMENT 'MYSQL 等',
    `jdbc_url` VARCHAR(255) NOT NULL COMMENT 'JDBC 地址',
    `username` VARCHAR(128) NOT NULL COMMENT '用户名',
    `password_cipher` VARCHAR(255) NOT NULL COMMENT '加密密码',
    `schema_name` VARCHAR(64) NULL COMMENT 'Schema',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-未测试 1-已启用 2-已停用',
    `last_check_time` DATETIME NULL COMMENT '最近校验时间',
    `owner_user_id` BIGINT NULL COMMENT '负责人',
    `remark` VARCHAR(255) NULL COMMENT '备注',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `updater_id` BIGINT NULL COMMENT '更新人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_datasource_code` (`datasource_code`),
    INDEX `idx_status_owner` (`status`, `owner_user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源表';

CREATE TABLE IF NOT EXISTS `ea_archive_group` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `parent_id` BIGINT NULL COMMENT '父分组 ID',
    `group_code` VARCHAR(64) NOT NULL COMMENT '分组编码，唯一',
    `group_name` VARCHAR(64) NOT NULL COMMENT '分组名称',
    `group_path` VARCHAR(255) NULL COMMENT '层级路径',
    `group_level` INT NOT NULL DEFAULT 1 COMMENT '层级',
    `source_datasource_id` BIGINT NOT NULL COMMENT '源数据源',
    `target_datasource_id` BIGINT NOT NULL COMMENT '目标数据源',
    `owner_user_id` BIGINT NULL COMMENT '负责人',
    `enable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `notify_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '0-关闭通知 1-开启通知',
    `notify_channel` VARCHAR(16) NULL COMMENT '通知渠道：FEISHU/WECOM/IN_APP',
    `notify_webhook_url` VARCHAR(500) NULL COMMENT '通知 webhook 地址',
    `remark` VARCHAR(255) NULL COMMENT '备注',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `updater_id` BIGINT NULL COMMENT '更新人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_group_code` (`group_code`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_source_datasource` (`source_datasource_id`),
    INDEX `idx_target_datasource` (`target_datasource_id`),
    INDEX `idx_enable_status` (`enable_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档分组表';

CREATE TABLE IF NOT EXISTS `ea_archive_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号，唯一',
    `group_id` BIGINT NOT NULL COMMENT '分组 ID',
    `trigger_type` VARCHAR(16) NOT NULL COMMENT 'MANUAL/SCHEDULE/RETRY',
    `execute_status` VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT 'WAITING/RUNNING/SUCCESS/FAILED/CANCELLING/CANCELLED',
    `start_time` DATETIME NULL COMMENT '开始时间',
    `end_time` DATETIME NULL COMMENT '结束时间',
    `processed_records` BIGINT NOT NULL DEFAULT 0 COMMENT '已处理条数',
    `processed_speed` DECIMAL(18,2) NULL COMMENT '当前速率',
    `current_rule_id` BIGINT NULL COMMENT '当前明细 ID',
    `current_item_type` VARCHAR(16) NULL COMMENT '当前明细类型 ID/TIME',
    `heartbeat_time` DATETIME NULL COMMENT '最近心跳',
    `cancel_reason` VARCHAR(255) NULL COMMENT '取消原因',
    `error_msg` VARCHAR(1000) NULL COMMENT '错误摘要',
    `trigger_user_id` BIGINT NULL COMMENT '触发人',
    `finished_flag` BIGINT NULL COMMENT '终态幂等标记',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_task_no` (`task_no`),
    INDEX `idx_group_status` (`group_id`, `execute_status`),
    INDEX `idx_status_heartbeat` (`execute_status`, `heartbeat_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务表';

CREATE TABLE IF NOT EXISTS `ea_archive_task_detail` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务 ID',
    `rule_id` BIGINT NOT NULL COMMENT '明细 ID',
    `item_type` VARCHAR(16) NOT NULL COMMENT 'ID/TIME',
    `execute_status` VARCHAR(16) NOT NULL DEFAULT 'WAITING' COMMENT '明细状态',
    `processed_records` BIGINT NOT NULL DEFAULT 0 COMMENT '已处理条数',
    `processed_speed` DECIMAL(18,2) NULL COMMENT '处理速率',
    `start_time` DATETIME NULL COMMENT '开始时间',
    `end_time` DATETIME NULL COMMENT '结束时间',
    `error_msg` VARCHAR(1000) NULL COMMENT '失败摘要',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_rule_id` (`rule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务明细表';

CREATE TABLE IF NOT EXISTS `ea_archive_task_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务 ID',
    `snapshot_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '快照时间',
    `execute_phase` VARCHAR(64) NULL COMMENT '当前阶段',
    `processed_records` BIGINT NOT NULL DEFAULT 0 COMMENT '已处理条数',
    `processed_speed` DECIMAL(18,2) NULL COMMENT '当前速率',
    `current_rule_id` BIGINT NULL COMMENT '当前明细 ID',
    `current_item_type` VARCHAR(16) NULL COMMENT '当前明细类型 ID/TIME',
    `heartbeat_flag` TINYINT NOT NULL DEFAULT 1 COMMENT '心跳正常标记',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_snapshot_time` (`snapshot_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务进度表';

CREATE TABLE IF NOT EXISTS `ea_archive_task_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `task_id` BIGINT NOT NULL COMMENT '任务 ID',
    `rule_id` BIGINT NULL COMMENT '明细 ID',
    `item_type` VARCHAR(16) NULL COMMENT '明细类型 ID/TIME',
    `log_type` VARCHAR(32) NOT NULL COMMENT 'START/PROGRESS/FINISH/ERROR/CANCEL',
    `log_level` VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'INFO/WARN/ERROR',
    `execute_phase` VARCHAR(64) NULL COMMENT '执行阶段',
    `log_content` TEXT NOT NULL COMMENT '日志内容',
    `processed_count` BIGINT NULL COMMENT '已处理数',
    `process_speed` DECIMAL(18,2) NULL COMMENT '处理速率',
    `log_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志时间',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_rule_id` (`rule_id`),
    INDEX `idx_log_type` (`log_type`),
    INDEX `idx_log_time` (`log_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务日志表';

-- 监控告警模块
CREATE TABLE IF NOT EXISTS `ea_monitor_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `rule_name` VARCHAR(64) NOT NULL COMMENT '监控规则名称',
    `group_id` BIGINT NULL COMMENT '关联分组，可空',
    `metric_code` VARCHAR(32) NOT NULL COMMENT 'FAILURE_COUNT/HEARTBEAT_TIMEOUT/SPEED_LOW',
    `threshold_value` DECIMAL(18,2) NOT NULL COMMENT '阈值',
    `compare_type` VARCHAR(8) NOT NULL COMMENT 'GT/LT/GTE/LTE',
    `channel_type` VARCHAR(32) NOT NULL COMMENT 'EMAIL/WEBHOOK/WECOM',
    `webhook_url` VARCHAR(255) NULL COMMENT '渠道地址',
    `silence_minutes` INT NOT NULL DEFAULT 5 COMMENT '静默时间',
    `enable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '启停状态',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` BIGINT NULL COMMENT '创建人ID',
    `updater_id` BIGINT NULL COMMENT '更新人ID',
    `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_id` (`group_id`),
    INDEX `idx_enable_status` (`enable_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监控规则表';

CREATE TABLE IF NOT EXISTS `ea_alert_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `monitor_rule_id` BIGINT NOT NULL COMMENT '监控规则 ID',
    `task_id` BIGINT NULL COMMENT '关联任务',
    `alert_level` VARCHAR(16) NOT NULL COMMENT 'INFO/WARN/CRITICAL',
    `alert_status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/NOTIFIED/RECOVERED/CLOSED',
    `alert_content` VARCHAR(1000) NOT NULL COMMENT '告警内容',
    `notify_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待发送 1-成功 2-失败',
    `trigger_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '触发时间',
    `recover_time` DATETIME NULL COMMENT '恢复时间',
    `handler_user_id` BIGINT NULL COMMENT '处理人',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_monitor_rule_id` (`monitor_rule_id`),
    INDEX `idx_task_id` (`task_id`),
    INDEX `idx_alert_status` (`alert_status`),
    INDEX `idx_trigger_time` (`trigger_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警事件表';

-- 初始化数据
INSERT INTO `sys_user` (`username`, `password`, `real_name`, `mobile`, `email`, `role_code`, `status`, `remark`) VALUES
('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '系统管理员', '13800000000', 'admin@example.com', 'platform_admin', 0, '系统管理员账户');

INSERT INTO `sys_role` (`role_code`, `role_name`, `status`, `data_scope_type`, `remark`) VALUES
('platform_admin', '平台管理员', 0, 'ALL', '平台管理员，拥有所有权限'),
('archive_admin', '归档管理员', 0, 'ASSIGNED', '归档管理员，负责归档配置管理'),
('auditor', '审计员', 0, 'VIEW', '审计员，仅查看权限'),
('observer', '观察员', 0, 'VIEW', '观察员，仅查看基础信息');

INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES
(1, 1);

-- 权限树结构
INSERT INTO `sys_permission` (`permission_code`, `permission_name`, `permission_type`, `parent_id`, `route_path`, `component`, `sort_no`, `status`) VALUES
-- 系统管理
('system', '系统管理', 'MENU', NULL, '/system', 'Layout', 100, 0),
('system:user', '用户管理', 'MENU', 1, '/system/user', 'system/user/index', 101, 0),
('system:user:view', '查看用户', 'BUTTON', 2, NULL, NULL, 102, 0),
('system:user:add', '新增用户', 'BUTTON', 2, NULL, NULL, 103, 0),
('system:user:edit', '编辑用户', 'BUTTON', 2, NULL, NULL, 104, 0),
('system:user:delete', '删除用户', 'BUTTON', 2, NULL, NULL, 105, 0),
('system:role', '角色管理', 'MENU', 1, '/system/role', 'system/role/index', 110, 0),
('system:role:view', '查看角色', 'BUTTON', 7, NULL, NULL, 111, 0),
('system:role:add', '新增角色', 'BUTTON', 7, NULL, NULL, 112, 0),
('system:role:edit', '编辑角色', 'BUTTON', 7, NULL, NULL, 113, 0),
('system:permission', '权限管理', 'MENU', 1, '/system/permission', 'system/permission/index', 120, 0),
('system:permission:view', '查看权限', 'BUTTON', 11, NULL, NULL, 121, 0),
('system:log', '操作日志', 'MENU', 1, '/system/log', 'system/log/index', 130, 0),
('system:log:view', '查看日志', 'BUTTON', 13, NULL, NULL, 131, 0),

-- 归档管理
('archive', '归档管理', 'MENU', NULL, '/archive', 'Layout', 200, 0),
('archive:datasource', '数据源管理', 'MENU', 15, '/archive/datasource', 'archive/datasource/index', 201, 0),
('archive:datasource:view', '查看数据源', 'BUTTON', 16, NULL, NULL, 202, 0),
('archive:datasource:add', '新增数据源', 'BUTTON', 16, NULL, NULL, 203, 0),
('archive:datasource:edit', '编辑数据源', 'BUTTON', 16, NULL, NULL, 204, 0),
('archive:datasource:test', '测试连接', 'BUTTON', 16, NULL, NULL, 205, 0),
('archive:group', '分组管理', 'MENU', 15, '/archive/group', 'archive/group/index', 210, 0),
('archive:group:view', '查看分组', 'BUTTON', 21, NULL, NULL, 211, 0),
('archive:group:add', '新增分组', 'BUTTON', 21, NULL, NULL, 212, 0),
('archive:group:edit', '编辑分组', 'BUTTON', 21, NULL, NULL, 213, 0),
('archive:rule', '规则管理', 'MENU', 15, '/archive/rule', 'archive/rule/index', 220, 0),
('archive:rule:view', '查看规则', 'BUTTON', 26, NULL, NULL, 221, 0),
('archive:rule:add', '新增规则', 'BUTTON', 26, NULL, NULL, 222, 0),
('archive:rule:edit', '编辑规则', 'BUTTON', 26, NULL, NULL, 223, 0),
('archive:rule:validate', '校验规则', 'BUTTON', 26, NULL, NULL, 224, 0),
('archive:task', '任务管理', 'MENU', 15, '/archive/task', 'archive/task/index', 230, 0),
('archive:task:view', '查看任务', 'BUTTON', 31, NULL, NULL, 231, 0),
('archive:task:trigger', '触发任务', 'BUTTON', 31, NULL, NULL, 232, 0),
('archive:task:cancel', '取消任务', 'BUTTON', 31, NULL, NULL, 233, 0),

-- 监控中心
('monitor', '监控中心', 'MENU', NULL, '/monitor', 'Layout', 300, 0),
('monitor:dashboard', '监控大盘', 'MENU', 35, '/monitor/dashboard', 'monitor/dashboard/index', 301, 0),
('monitor:dashboard:view', '查看大盘', 'BUTTON', 36, NULL, NULL, 302, 0),
('monitor:rule', '监控规则', 'MENU', 35, '/monitor/rule', 'monitor/rule/index', 310, 0),
('monitor:rule:view', '查看监控规则', 'BUTTON', 38, NULL, NULL, 311, 0),
('monitor:rule:add', '新增监控规则', 'BUTTON', 38, NULL, NULL, 312, 0),
('monitor:rule:edit', '编辑监控规则', 'BUTTON', 38, NULL, NULL, 313, 0),
('monitor:alert', '告警中心', 'MENU', 35, '/monitor/alert', 'monitor/alert/index', 320, 0),
('monitor:alert:view', '查看告警', 'BUTTON', 42, NULL, NULL, 321, 0),
('monitor:alert:handle', '处理告警', 'BUTTON', 42, NULL, NULL, 322, 0);

-- 为平台管理员分配所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission` WHERE deleted = 0;

COMMIT;
