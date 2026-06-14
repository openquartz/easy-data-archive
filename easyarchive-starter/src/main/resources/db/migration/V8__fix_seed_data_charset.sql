SET NAMES utf8mb4;

-- Repair seed data that was imported through a latin1 client session.
UPDATE `sys_user`
SET `real_name` = '系统管理员',
    `remark` = '系统管理员账户'
WHERE `username` = 'admin';

UPDATE `sys_role`
SET `role_name` = CASE `role_code`
        WHEN 'platform_admin' THEN '平台管理员'
        WHEN 'archive_admin' THEN '归档管理员'
        WHEN 'auditor' THEN '审计员'
        WHEN 'observer' THEN '观察员'
        ELSE `role_name`
    END,
    `remark` = CASE `role_code`
        WHEN 'platform_admin' THEN '平台管理员，拥有所有权限'
        WHEN 'archive_admin' THEN '归档管理员，负责归档配置管理'
        WHEN 'auditor' THEN '审计员，仅查看权限'
        WHEN 'observer' THEN '观察员，仅查看基础信息'
        ELSE `remark`
    END
WHERE `role_code` IN ('platform_admin', 'archive_admin', 'auditor', 'observer');

UPDATE `sys_permission`
SET `permission_name` = CASE `permission_code`
        WHEN 'system' THEN '系统管理'
        WHEN 'system:user' THEN '用户管理'
        WHEN 'system:user:view' THEN '查看用户'
        WHEN 'system:user:add' THEN '新增用户'
        WHEN 'system:user:edit' THEN '编辑用户'
        WHEN 'system:user:delete' THEN '删除用户'
        WHEN 'system:role' THEN '角色管理'
        WHEN 'system:role:view' THEN '查看角色'
        WHEN 'system:role:add' THEN '新增角色'
        WHEN 'system:role:edit' THEN '编辑角色'
        WHEN 'system:permission' THEN '权限管理'
        WHEN 'system:permission:view' THEN '查看权限'
        WHEN 'system:log' THEN '操作日志'
        WHEN 'system:log:view' THEN '查看日志'
        WHEN 'archive' THEN '归档管理'
        WHEN 'archive:datasource' THEN '数据源管理'
        WHEN 'archive:datasource:view' THEN '查看数据源'
        WHEN 'archive:datasource:add' THEN '新增数据源'
        WHEN 'archive:datasource:edit' THEN '编辑数据源'
        WHEN 'archive:datasource:test' THEN '测试连接'
        WHEN 'archive:group' THEN '分组管理'
        WHEN 'archive:group:view' THEN '查看分组'
        WHEN 'archive:group:add' THEN '新增分组'
        WHEN 'archive:group:edit' THEN '编辑分组'
        WHEN 'archive:rule' THEN '规则管理'
        WHEN 'archive:rule:view' THEN '查看规则'
        WHEN 'archive:rule:add' THEN '新增规则'
        WHEN 'archive:rule:edit' THEN '编辑规则'
        WHEN 'archive:rule:validate' THEN '校验规则'
        WHEN 'archive:task' THEN '任务管理'
        WHEN 'archive:task:view' THEN '查看任务'
        WHEN 'archive:task:trigger' THEN '触发任务'
        WHEN 'archive:task:cancel' THEN '取消任务'
        WHEN 'monitor' THEN '监控中心'
        WHEN 'monitor:dashboard' THEN '监控大盘'
        WHEN 'monitor:dashboard:view' THEN '查看大盘'
        WHEN 'monitor:rule' THEN '监控规则'
        WHEN 'monitor:rule:view' THEN '查看监控规则'
        WHEN 'monitor:rule:add' THEN '新增监控规则'
        WHEN 'monitor:rule:edit' THEN '编辑监控规则'
        WHEN 'monitor:alert' THEN '告警中心'
        WHEN 'monitor:alert:view' THEN '查看告警'
        WHEN 'monitor:alert:handle' THEN '处理告警'
        ELSE `permission_name`
    END
WHERE `permission_code` IN (
    'system', 'system:user', 'system:user:view', 'system:user:add', 'system:user:edit', 'system:user:delete',
    'system:role', 'system:role:view', 'system:role:add', 'system:role:edit',
    'system:permission', 'system:permission:view', 'system:log', 'system:log:view',
    'archive', 'archive:datasource', 'archive:datasource:view', 'archive:datasource:add',
    'archive:datasource:edit', 'archive:datasource:test', 'archive:group', 'archive:group:view',
    'archive:group:add', 'archive:group:edit', 'archive:rule', 'archive:rule:view',
    'archive:rule:add', 'archive:rule:edit', 'archive:rule:validate', 'archive:task',
    'archive:task:view', 'archive:task:trigger', 'archive:task:cancel',
    'monitor', 'monitor:dashboard', 'monitor:dashboard:view', 'monitor:rule',
    'monitor:rule:view', 'monitor:rule:add', 'monitor:rule:edit',
    'monitor:alert', 'monitor:alert:view', 'monitor:alert:handle'
);
