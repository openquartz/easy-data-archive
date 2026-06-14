# EasyArchive 项目架构设计

## 1. 概述

EasyArchive 是一个企业级数据归档与迁移平台，用于将业务数据从生产库按规则批量迁移到目标库，并可选清理源数据。平台提供 Web UI 管理数据源、归档分组、归档任务，支持定时通知与站内信。

**技术栈:**
- 后端: Java 11 + Spring Boot 2.7.18 + MyBatis 3.5.0
- 前端: Vue 3 + TypeScript + Vite + Pinia + Vue Router
- 数据库: MySQL 5.x (HikariCP 连接池)
- 安全: Spring Security + JWT (无状态会话)
- 通知: 飞书 Webhook / 企业微信 Webhook / 站内信

## 2. 模块结构

```
easy-archive/
├── easyarchive-common/          # 公共模块: API 接口、枚举、工具类、基础实体
├── easyarchive-core/            # 核心业务: 执行引擎、表达式引擎、MySQL 实现
└── easyarchive-starter/         # 启动模块: Spring Boot 应用、REST API、安全、通知
```

### 依赖关系

```
easyarchive-starter → easyarchive-core → easyarchive-common
```

- `easyarchive-common`: 不含 Spring 依赖，纯 API 定义
- `easyarchive-core`: 核心执行逻辑，使用 MyBatis 但无 Web 依赖
- `easyarchive-starter`: Spring Boot 应用层，依赖所有模块

## 3. 分层架构

```
┌─────────────────────────────────────────────────────┐
│                   Vue 3 Frontend                    │
│            (Pinia / Vue Router / Axios)              │
└──────────────────┬──────────────────────────────────┘
                   │ REST API (JSON)
┌──────────────────▼──────────────────────────────────┐
│              REST API Layer (Starter)                │
│  Controller → Service → Mapper/Repository           │
│  (Security/JWT/Auth/OperationLog AOP)                │
├─────────────────────────────────────────────────────┤
│              Core Execution Layer                    │
│  ArchiveGroupExecutor → ArchiveExecutor → SyncExecutor│
│  PageSource (MySQL) → Sink (MySQL)                  │
│  ExpressionEngine + ExecutorRegistry                 │
│  EventPublisher + Listener                          │
├─────────────────────────────────────────────────────┤
│            Persistence Layer                         │
│  MyBatis Mapper → JDBC (HikariCP)                   │
└─────────────────────────────────────────────────────┘
```

## 4. 核心执行流程

```
用户触发归档分组
       │
       ▼
ArchiveGroupController.trigger(groupId)
       │
       ▼
ArchiveGroupExecutionService → 创建 ArchiveGroupExecuteTask (WAITING)
       │
       ▼
ArchiveGroupExecutor.run()
       │── 加载 ArchiveGroupItem (按优先级排序)
       │── 遍历每个 ArchiveGroupItem:
       │     │
       │     ▼
       │  ArchiveExecutor.run()
       │     │
       │     ├── ID 类型规则: 按 ID 范围分片滚动
       │     │   ┌──────────┐    ┌──────────┐    ┌──────────┐
       │     │   │ startId  │───→│ +stepRds │───→│ endId    │
       │     │   │ ~curEndId│    │ ~curEndId│    │ ~endId   │
       │     │   └──────────┘    └──────────┘    └──────────┘
       │     │
       │     ├── TIME 类型规则: 按时间窗口分片滚动
       │     │   ┌──────────┐    ┌──────────┐
       │     │   │ startTime│───→│ +stepMin │───→│ keepDay前│
       │     │   │ ~+1hour  │    │ ~+1hour  │
       │     │   └──────────┘    └──────────┘
       │     │
       │     │     ┌─────────┐
       │     │     │MysqlSource│── SELECT * FROM t WHERE ... LIMIT N
       │     │     └────┬────┘
       │     │          │ DataIterator
       │     │     ┌────▼────┐
       │     │     │SyncExec │── for each batch:
       │     │     │utor     │   1. sink.write(batch)  → REPLACE INTO
       │     │     │         │   2. reader.clean(batch) → DELETE FROM
       │     │     └─────────┘
       │     │
       │     │ 发布事件: RuleStart → TaskProgress → RuleEnd
       │     ▼
       ▼
  完成 → TASK_END 事件
```

## 5. 表达式引擎

### 5.1 表达式格式

支持 `$cmd param1 param2 {nested}$` 格式的嵌套表达式。

格式: `$<command> <param1> <param2> ... {<nested_expression>}$`

### 5.2 注册命令

| 命令 | 功能 |
|------|------|
| `const` | 常量值 |
| `time` | 当前时间 |
| `time_add` | 时间加减 |
| `fix` | 定长补零 |
| `env` | 环境变量 |
| `rand_n` | 随机数字 |
| `rand_c` | 随机字母数字 |
| `func` | 函数调用 |
| `mod` | 取模 |
| `sql` | SQL 查询 |
| `spel` | Spring Expression Language |
| `hash_mod` | Hash 取模 |

### 5.3 执行流程

```
ExpressionService.parse("$table_$date${time}$")
       │
       ▼
CommandTreeParser.parseExpr()
       │
       ▼
CommandNode (AST 树)
       │
       ▼
ExpressionEngine.execute()
       │
       ▼
ExecutorRegistry → 查找对应 CommandExecutor
       │
       ▼
Result
```

## 6. 事件系统

### 6.1 事件类型

| 事件 | 触发时机 | 数据 |
|------|----------|------|
| `TASK_START` | 分组执行开始 | 规则数量 |
| `RULE_START` | 单条规则开始 | 源表、目标表、规则类型 |
| `TASK_PROGRESS` | 进度更新 (定时) | 已处理行数、耗时 |
| `RULE_END` | 单条规则完成 | 处理行数、耗时、错误信息 |
| `TASK_END` | 分组执行完成 | 总行数、总耗时、是否成功 |

### 6.2 事件监听器

```
DefaultArchiveEventPublisher
    │
    ├── DbArchiveLogListener (持久化到 ea_archive_task_log)
    ├── ArchiveNotificationListener (飞书/企微通知)
    └── ArchiveInAppNotificationListener (站内信)
```

## 7. 权限与安全

### 7.1 角色体系

| 角色 | 说明 |
|------|------|
| `platform_admin` | 平台管理员 (全部权限) |
| `archive_admin` | 归档管理员 (归档配置管理) |
| `normal_user` | 普通用户 (默认角色) |

### 7.2 数据源权限

| 权限级别 | 说明 |
|----------|------|
| `MANAGE` | 管理 (包含 USE) |
| `USE` | 使用 |

### 7.3 平台能力 (PlatformCapabilityEnum)

用户 CRUD、数据源管理、归档分组操作、任务查看、仪表盘、通知、操作日志等能力均由此枚举定义。

### 7.4 安全配置

- JWT 无状态认证
- BCrypt 密码加密
- CORS 仅允许 localhost
- 除登录接口外所有 API 均需认证

## 8. 通知系统

### 8.1 通知渠道

- **飞书**: Markdown Webhook
- **企业微信**: Markdown Webhook
- **站内信**: 数据库持久化 + 前端轮询

### 8.2 通知流程

```
TaskEndEvent
    │
    ▼
ArchiveNotificationService.notifyTaskCompletion()
    │── 检查分组 notify_enabled
    │── 构建 ArchiveNotification 对象
    │── 渲染消息
    │── 通过 NotificationClient 发送
```

## 9. 配置说明

### 9.1 后端配置 (application.yml)

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `archive.task.thread-pool.core-pool-size` | 10 | 线程池核心数 |
| `archive.task.thread-pool.max-pool-size` | 50 | 线程池最大数 |
| `archive.task.thread-pool.queue-capacity` | 100 | 队列容量 |
| `archive.datasource.test-query` | SELECT 1 | 连接测试语句 |
| `archive.rule.default-batch-size` | 1000 | 默认批次大小 |
| `archive.rule.default-pause-ms` | 100 | 默认停顿毫秒 |
| `sync.reader.load.max.rows` | 5000 | 单次最大读取行数 |
| `sync.archive.step.interval.time` | 50 | 批处理间隔(ms) |
| `sync.log.retention-days` | 30 | 日志保留天数 |

### 9.2 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `MYSQL_HOST` | localhost | MySQL 主机 |
| `MYSQL_PORT` | 3306 | MySQL 端口 |
| `MYSQL_DATABASE` | openquartz | 数据库名 |
| `MYSQL_USER` | root | 用户名 |
| `MYSQL_PASSWORD` | 123456 | 密码 |
| `MYSQL_TIMEZONE` | Asia/Shanghai | 时区 |

## 10. 定时任务

| 任务 | 说明 |
|------|------|
| `ArchiveLogCleanupTask` | 清理过期归档任务日志 |
| `InAppNotificationCleanupTask` | 清理过期站内信 |

## 11. 部署架构

```
┌──────────┐     ┌──────────────┐     ┌──────────┐
│  Browser  │────▶│  easyarchive- │────▶│  MySQL   │
│  Vue 3   │◀────│  ui (Vite)    │     │  8789    │
└──────────┘     └──────────────┘     └──────────┘
                            │
                     ┌──────▼───────┐
                     │ Spring Boot  │────▶ 飞书/企微 Webhook
                     │  :8789       │
                     └──────────────┘
```