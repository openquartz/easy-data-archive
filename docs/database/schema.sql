-- EasyArchive 数据库表结构
-- MySQL 5.7+

-- 创建数据库
CREATE DATABASE IF NOT EXISTS easy_archive DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE easy_archive;

-- 归档连接配置表
CREATE TABLE archive_connection (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    connect_code VARCHAR(100) NOT NULL UNIQUE COMMENT '连接编码，唯一',
    connect_type VARCHAR(50) NOT NULL DEFAULT 'MYSQL' COMMENT '连接类型。MYSQL、ES等',
    url VARCHAR(500) NOT NULL COMMENT '数据库连接URL',
    username VARCHAR(100) NOT NULL COMMENT '用户名',
    password VARCHAR(200) NOT NULL COMMENT '密码',
    status INT NOT NULL DEFAULT 0 COMMENT '0-未测试，1-正常，2-异常',
    remark VARCHAR(500) COMMENT '备注',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id VARCHAR(50) COMMENT '创建人ID',
    updater_id VARCHAR(50) COMMENT '更新人ID',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0代表正常数据，其他为逻辑删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档连接配置表';

-- 归档分组表
CREATE TABLE archive_group (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_code VARCHAR(100) NOT NULL UNIQUE COMMENT '分组编码，唯一',
    group_name VARCHAR(200) NOT NULL COMMENT '分组名称',
    source_connection_id INT NOT NULL COMMENT '源库连接ID',
    target_connection_id INT COMMENT '目标库连接ID，可选',
    owner_user_id BIGINT NOT NULL COMMENT '所属人ID',
    enable_status INT NOT NULL DEFAULT 0 COMMENT '0-启用，1-禁用',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id VARCHAR(50) COMMENT '创建人ID',
    updater_id VARCHAR(50) COMMENT '更新人ID',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0代表正常数据，其他为逻辑删除',

    INDEX idx_group_code (group_code),
    INDEX idx_enable_status (enable_status),
    INDEX idx_owner_user (owner_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档分组表';

-- 归档配置表（按时间归档）
CREATE TABLE archive_config_by_time (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '分组ID',
    source_table VARCHAR(200) NOT NULL COMMENT '来源表名',
    target_table VARCHAR(200) NOT NULL COMMENT '目标表名',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    fetch_sql TEXT NOT NULL COMMENT '执行SQL',
    delete_where VARCHAR(1000) COMMENT '删除条件',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    keep_day INT NOT NULL DEFAULT 30 COMMENT '保持多少天',
    step_minutes INT NOT NULL DEFAULT 60 COMMENT '滚动步长时间，单位：分钟',
    step_count INT NOT NULL DEFAULT 1000 COMMENT '步长',
    pause_ms INT COMMENT '规则停顿时间，单位毫秒，为空时走全局配置',
    enable_clean INT NOT NULL DEFAULT 0 COMMENT '0-启用清理源数据，1-不启用清理源数据',
    enable_write INT NOT NULL DEFAULT 0 COMMENT '0-启用写入目标数据，1-不启用',
    enable_status INT NOT NULL DEFAULT 0 COMMENT '0-启用，1-禁用',
    id_column VARCHAR(50) NOT NULL DEFAULT 'ID' COMMENT 'ID字段名，默认为ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id VARCHAR(50) COMMENT '创建人ID',
    updater_id VARCHAR(50) COMMENT '更新人ID',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0代表正常数据，其他为逻辑删除',

    INDEX idx_group_id (group_id),
    INDEX idx_enable_status (enable_status),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档配置表-按时间';

-- 归档配置表（按ID归档）
CREATE TABLE archive_config_by_id (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '分组ID',
    source_table VARCHAR(200) NOT NULL COMMENT '来源表名',
    target_table VARCHAR(200) NOT NULL COMMENT '目标表名',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    fetch_sql TEXT NOT NULL COMMENT '执行SQL',
    delete_where VARCHAR(1000) COMMENT '删除条件',
    start_id VARCHAR(50) NOT NULL DEFAULT '0' COMMENT '开始ID',
    end_id VARCHAR(50) NOT NULL DEFAULT '9223372036854775807' COMMENT '结束ID',
    step_count INT NOT NULL DEFAULT 1000 COMMENT '步长',
    step_rounds INT NOT NULL DEFAULT 5000 COMMENT '滚动步长',
    pause_ms INT COMMENT '规则停顿时间，单位毫秒，为空时走全局配置',
    enable_clean INT NOT NULL DEFAULT 0 COMMENT '0-启用清理源数据，1-不启用清理源数据',
    enable_write INT NOT NULL DEFAULT 0 COMMENT '0-启用写入目标数据，1-不启用',
    enable_status INT NOT NULL DEFAULT 0 COMMENT '0-启用，1-禁用',
    id_column VARCHAR(50) NOT NULL DEFAULT 'ID' COMMENT 'ID字段名，默认为ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id VARCHAR(50) COMMENT '创建人ID',
    updater_id VARCHAR(50) COMMENT '更新人ID',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0代表正常数据，其他为逻辑删除',

    INDEX idx_group_id (group_id),
    INDEX idx_enable_status (enable_status),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档配置表-按ID';

-- 统一归档配置表（用于DbArchiveRuleLoader）
CREATE TABLE archive_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '分组ID',
    archive_type VARCHAR(20) NOT NULL COMMENT '归档类型：TIME-按时间，ID-按ID',
    source_table VARCHAR(200) NOT NULL COMMENT '来源表名',
    target_table VARCHAR(200) NOT NULL COMMENT '目标表名',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    fetch_sql TEXT NOT NULL COMMENT '执行SQL',
    delete_where VARCHAR(1000) COMMENT '删除条件',

    -- 按时间归档相关字段
    start_time DATETIME COMMENT '开始时间',
    keep_day INT COMMENT '保持多少天',
    step_minutes INT COMMENT '滚动步长时间，单位：分钟',

    -- 按ID归档相关字段
    start_id VARCHAR(50) COMMENT '开始ID',
    end_id VARCHAR(50) COMMENT '结束ID',
    step_rounds INT COMMENT '滚动步长',

    -- 通用字段
    step_count INT NOT NULL DEFAULT 1000 COMMENT '步长',
    pause_ms INT COMMENT '规则停顿时间，单位毫秒，为空时走全局配置',
    enable_clean INT NOT NULL DEFAULT 0 COMMENT '0-启用清理源数据，1-不启用清理源数据',
    enable_write INT NOT NULL DEFAULT 0 COMMENT '0-启用写入目标数据，1-不启用',
    enable_status INT NOT NULL DEFAULT 0 COMMENT '0-启用，1-禁用',
    id_column VARCHAR(50) NOT NULL DEFAULT 'ID' COMMENT 'ID字段名，默认为ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id VARCHAR(50) COMMENT '创建人ID',
    updater_id VARCHAR(50) COMMENT '更新人ID',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0代表正常数据，其他为逻辑删除',

    INDEX idx_group_id (group_id),
    INDEX idx_archive_type (archive_type),
    INDEX idx_enable_status (enable_status),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一归档配置表';

-- 归档执行任务表
CREATE TABLE archive_group_execute_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    group_id BIGINT NOT NULL COMMENT '归档分组ID',
    start_time DATETIME COMMENT '执行开始时间',
    end_time DATETIME COMMENT '执行结束时间',
    execute_status INT NOT NULL DEFAULT 0 COMMENT '执行状态：0-等待，1-执行中，2-成功，3-失败',
    error_msg TEXT COMMENT '执行异常信息',
    processed_records BIGINT NOT NULL DEFAULT 0 COMMENT '已经处理记录数',
    processed_speed DECIMAL(10,2) COMMENT '处理速度（记录/秒）',
    heartbeat_time DATETIME COMMENT '最新心跳时间点',
    finished_flag BIGINT NOT NULL DEFAULT 0 COMMENT '是否已完成标记，0-为未完成，否则为id',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id VARCHAR(50) COMMENT '创建人ID',
    updater_id VARCHAR(50) COMMENT '更新人ID',
    deleted BIGINT NOT NULL DEFAULT 0 COMMENT '0代表正常数据，其他为逻辑删除',

    INDEX idx_group_id (group_id),
    INDEX idx_execute_status (execute_status),
    INDEX idx_heartbeat_time (heartbeat_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档执行任务表';

-- 归档任务日志表
CREATE TABLE archive_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    table_name VARCHAR(200) NOT NULL COMMENT '表名',
    archive_type VARCHAR(20) NOT NULL COMMENT '归档类型：TIME-按时间，ID-按ID',
    start_range VARCHAR(100) COMMENT '开始范围',
    end_range VARCHAR(100) COMMENT '结束范围',
    processed_rows INT NOT NULL DEFAULT 0 COMMENT '处理行数',
    status INT NOT NULL DEFAULT 0 COMMENT '状态：0-成功，1-失败',
    error_message TEXT COMMENT '错误信息',
    execute_time BIGINT COMMENT '执行时间（毫秒）',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_task_id (task_id),
    INDEX idx_table_name (table_name),
    INDEX idx_created_time (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务日志表';

-- 插入示例数据
INSERT INTO archive_connection (connect_code, connect_type, url, username, password, status, remark) VALUES
('SOURCE_DB', 'MYSQL', 'jdbc:mysql://localhost:3306/source_db?useUnicode=true&characterEncoding=utf8&useSSL=false', 'root', 'password', 1, '源数据库'),
('TARGET_DB', 'MYSQL', 'jdbc:mysql://localhost:3306/target_db?useUnicode=true&characterEncoding=utf8&useSSL=false', 'root', 'password', 1, '目标数据库'),
('CONFIG_DB', 'MYSQL', 'jdbc:mysql://localhost:3306/config_db?useUnicode=true&characterEncoding=utf8&useSSL=false', 'root', 'password', 1, '配置数据库');

INSERT INTO archive_group (group_code, group_name, source_connection_id, target_connection_id, owner_user_id, enable_status) VALUES
('USER_DATA_GROUP', '用户数据归档组', 1, 2, 1001, 0);

-- 示例：按时间归档配置
INSERT INTO archive_config (group_id, archive_type, source_table, target_table, priority, fetch_sql, start_time, keep_day, step_minutes, step_count, enable_clean, enable_write, enable_status, id_column) VALUES
(1, 'TIME', 'user_orders', 'user_orders_archive', 1, 'SELECT * FROM user_orders WHERE create_time BETWEEN ? AND ?', '2020-01-01 00:00:00', 365, 60, 1000, 0, 0, 0, 'id');

-- 示例：按ID归档配置
INSERT INTO archive_config (group_id, archive_type, source_table, target_table, priority, fetch_sql, start_id, end_id, step_count, step_rounds, enable_clean, enable_write, enable_status, id_column) VALUES
(1, 'ID', 'user_logs', 'user_logs_archive', 2, 'SELECT * FROM user_logs WHERE id BETWEEN ? AND ?', '0', '1000000', 1000, 5000, 0, 0, 0, 'id');

-- 创建视图：归档配置视图（合并时间和ID归档配置）
CREATE VIEW v_archive_config AS
SELECT
    id,
    group_id,
    'TIME' as archive_type,
    source_table,
    target_table,
    priority,
    fetch_sql,
    delete_where,
    start_time,
    keep_day,
    step_minutes,
    NULL as start_id,
    NULL as end_id,
    NULL as step_rounds,
    step_count,
    pause_ms,
    enable_clean,
    enable_write,
    enable_status,
    id_column
FROM archive_config_by_time
WHERE enable_status = 0 AND deleted = 0

UNION ALL

SELECT
    id,
    group_id,
    'ID' as archive_type,
    source_table,
    target_table,
    priority,
    fetch_sql,
    delete_where,
    NULL as start_time,
    NULL as keep_day,
    NULL as step_minutes,
    start_id,
    end_id,
    step_rounds,
    step_count,
    pause_ms,
    enable_clean,
    enable_write,
    enable_status,
    id_column
FROM archive_config_by_id
WHERE enable_status = 0 AND deleted = 0;