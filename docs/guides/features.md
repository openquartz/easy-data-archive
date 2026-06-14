# EasyArchive 功能特性详解

## 1. 数据源管理

### 1.1 功能概述

支持注册和管理多个 MySQL 数据源，包括源数据库（生产库）和目标数据库（归档库）。每个数据源可独立配置连接参数，并支持连接测试。

### 1.2 支持的数据源类型

| 类型 | 说明 | 用途 |
|------|------|------|
| MySQL 5.7+ | MySQL 5.7 系列 | 源/目标数据源 |
| MySQL 8.0+ | MySQL 8.0 系列 | 源/目标数据源 |

### 1.3 数据源状态

| 状态码 | 状态 | 说明 |
|--------|------|------|
| 0 | 禁用 | 不参与归档任务 |
| 1 | 启用 | 可被归档分组引用 |

### 1.4 数据源权限

系统支持数据源级别的权限隔离，每个用户被分配特定数据源的操作权限：

| 权限级别 | 权限说明 |
|----------|---------|
| MANAGE | 可编辑数据源配置、修改连接参数、创建归档分组 |
| USE | 仅可在该数据源上执行归档任务，不可修改配置 |

### 1.5 API 接口

```
GET    /api/v1/datasources              # 查询数据源列表
POST   /api/v1/datasources              # 创建数据源
PUT    /api/v1/datasources/{id}         # 更新数据源
DELETE /api/v1/datasources/{id}         # 删除数据源
POST   /api/v1/datasources/test         # 测试数据源连接
```

---

## 2. 归档分组管理

### 2.1 功能概述

归档分组是归档任务的核心组织单元。一个分组可以包含多个归档规则（按 ID 或按时间），触发分组后，系统会按规则优先级依次执行各规则。

### 2.2 分组结构

```
ArchiveGroup（归档分组）
  ├── name: 分组名称
  ├── description: 分组描述
  ├── enabled: 启用/禁用
  ├── owner: 负责人
  ├── notifyEnabled: 是否启用通知
  └── items: 归档规则列表
       ├── byIdRules: 按 ID 范围规则
       └── byTimeRules: 按时间范围规则
```

### 2.3 分组操作

| 操作 | 说明 |
|------|------|
| 新增分组 | 创建新的归档分组，设置基本信息 |
| 编辑分组 | 修改分组名称、描述、负责人、通知配置 |
| 启用/禁用 | 控制分组是否可被触发 |
| 删除分组 | 删除分组及其关联规则 |
| 变更负责人 | 修改分组的负责人 |
| 触发归档 | 手动触发分组执行归档任务 |
| 取消运行 | 取消正在运行的任务 |

### 2.4 API 接口

```
GET    /api/v1/archive/groups               # 查询分组列表
GET    /api/v1/archive/groups/page          # 分页查询分组
GET    /api/v1/archive/groups/tree          # 树形结构查询
POST   /api/v1/archive/groups               # 创建分组
GET    /api/v1/archive/groups/{id}          # 获取分组详情
PUT    /api/v1/archive/groups/{id}          # 更新分组
PATCH  /api/v1/archive/groups/{id}/status   # 修改分组状态
DELETE /api/v1/archive/groups/{id}          # 删除分组
PUT    /api/v1/archive/groups/{id}/owner    # 变更负责人
POST   /api/v1/archive/groups/{id}/trigger  # 触发归档
POST   /api/v1/archive/groups/{id}/cancel-active-task  # 取消运行任务
```

---

## 3. 归档规则

### 3.1 规则类型

系统支持两种归档规则类型：

#### 按 ID 范围归档

按主键 ID 范围分片，逐段迁移数据。适用于：
- 业务没有明确的时间字段
- 需要按 ID 顺序清理数据
- 分库分表场景

规则参数：
| 参数 | 说明 |
|------|------|
| sourceTable | 源表名称 |
| targetTable | 目标表名称 |
| startId | 起始 ID |
| endId | 结束 ID |
| stepRds | 分片步长 |

#### 按时间范围归档

按时间字段范围分片，逐小时/天迁移数据。适用于：
- 有明确时间维度的流水表
- 需要按时间段归档的场景
- 日志、交易记录等

规则参数：
| 参数 | 说明 |
|------|------|
| sourceTable | 源表名称 |
| targetTable | 目标表名称 |
| timeField | 时间字段名 |
| startTime | 起始时间 |
| keepDays | 保留天数（keepDays 之前的数据归档） |
| stepMinutes | 分片步长（分钟） |

### 3.2 规则优先级

当分组中包含多条规则时，系统按以下优先级执行：
1. 按时间规则优先于按 ID 规则
2. 同类型规则按创建顺序执行

### 3.3 API 接口

```
GET    /api/v1/archive-group-items/by-id      # 查询按 ID 规则
POST   /api/v1/archive-group-items/by-id      # 新增按 ID 规则
GET    /api/v1/archive-group-items/by-time    # 查询按时间规则
POST   /api/v1/archive-group-items/by-time    # 新增按时间规则
```

---

## 4. 任务管理与监控

### 4.1 任务状态

| 状态码 | 状态 | 说明 |
|--------|------|------|
| 0 | 等待 | 任务已创建，等待执行 |
| 1 | 运行中 | 任务正在执行 |
| 2 | 成功 | 任务执行完成，数据归档成功 |
| 3 | 失败 | 任务执行失败 |
| 4 | 取消中 | 用户已发起取消，正在中止 |
| 5 | 已取消 | 任务已被取消 |

### 4.2 任务执行流程

```
触发归档分组
    │
    ▼
创建执行任务 (WAITING)
    │
    ▼
ArchiveGroupExecutor 加载规则
    │
    ▼
遍历执行每个归档规则
    │
    ├── 发布 RuleStartEvent
    │
    ├── 按 ID/时间 分片
    │       │
    │       ├── SyncExecutor 并行读取 & 写入
    │       │   ├── MysqlSource 分页读取
    │       │   ├── MysqlSink 批量写入
    │       │   └── Cleaner 源数据清理
    │       │
    │       └── 发布 TaskProgressEvent（定期）
    │
    ▼
发布 RuleEndEvent
    │
    ▼
发布 TaskEndEvent
    │
    ▼
触发通知 & 持久化日志
```

### 4.3 任务执行日志

每个归档任务都会产生详细的执行日志，包括：
- 规则开始/结束事件
- 进度更新事件
- 执行阶段信息
- 错误详情

日志可配置保留天数，过期自动清理。

### 4.4 API 接口

```
GET    /api/v1/archive-tasks                     # 查询任务列表
POST   /api/v1/archive-tasks                     # 触发归档任务
GET    /api/v1/archive-tasks/{id}                # 获取任务详情
GET    /api/v1/archive-tasks/{id}/logs           # 获取任务日志
POST   /api/v1/archive-tasks/{id}/cancel         # 取消任务
```

---

## 5. 监控大盘

### 5.1 概览统计

监控大盘提供全局任务执行状态的可视化统计：

| 指标 | 说明 |
|------|------|
| 运行中任务数 | 当前正在执行的任务数量 |
| 成功任务数 | 已成功完成的任务数量 |
| 失败任务数 | 执行失败的任务数量 |
| 已启用数据源数 | 状态为启用的数据源数量 |
| 已禁用数据源数 | 状态为禁用的数据源数量 |
| 数据源总数 | 全部数据源数量 |

### 5.2 任务趋势图

展示最近 N 天的任务趋势，包括：
- 提交任务数（蓝色）
- 成功任务数（绿色）
- 失败任务数（红色）

### 5.3 最近任务与失败任务

分别展示最近的归档任务列表和所有失败任务列表，包含任务 ID、分组 ID、状态、处理记录数、处理速度、开始/结束时间等关键信息。

### 5.4 API 接口

```
GET /api/v1/dashboard/overview  # 获取全局统计概览
```

---

## 6. 表达式引擎

### 6.1 功能概述

系统内建表达式引擎，支持 `$cmd param1 param2 {nested}$` 格式的嵌套表达式。可用于字段映射、表名生成、数据转换等场景。

### 6.2 内置命令

| 命令 | 功能 | 示例 |
|------|------|------|
| `const` | 返回常量值 | `$const archive_2024$` |
| `time` | 当前时间 | `$time YYYY-MM-DD$` |
| `time_add` | 时间加减 | `$time_add 2024-01-01 1 day$` |
| `time_format` | 时间格式化 | `$time_format now YYYYMMDD$` |
| `fix` | 定长补零 | `$fix 123 5$` → `00123` |
| `env` | 环境变量 | `$env HOST$` |
| `rand_n` | 随机数字 | `$rand_n 5$` → 5 位随机数 |
| `rand_c` | 随机字母数字 | `$rand_c 8$` → 8 位随机串 |
| `func` | 函数调用 | `$func md5 hello$` |
| `mod` | 取模运算 | `$mod 100 7$` → `2` |
| `sql` | SQL 查询 | `$sql SELECT max(id) FROM t$` |
| `spel` | Spring 表达式 | `$spel #{T(java.util.UUID).randomUUID()}$` |
| `hash_mod` | Hash 取模 | `$hash_mod field 10$` |

### 6.3 表达式嵌套

支持嵌套表达式，可组合多个命令：

```
$table_archive_${time YYYYMMDD}$
$prefix_${const log}_${time YYYYMMDD}$
```

### 6.4 扩展方式

实现 `CommandExecutor` 接口并注册到 `ExecutorRegistry` 即可添加自定义命令：

```java
@Component
public class MyCustomExecutor implements CommandExecutor {
    @Override
    public Result execute(CommandNode node, Environment env) {
        // 自定义执行逻辑
    }
}
```

---

## 7. 消息通知

### 7.1 通知渠道

归档任务完成或失败时，系统可自动通过以下渠道通知相关人员：

| 渠道 | 协议 | 说明 |
|------|------|------|
| 飞书 | Webhook | 飞书群机器人 Markdown 消息 |
| 企业微信 | Webhook | 企业微信群机器人 Markdown 消息 |
| 站内消息 | 数据库 | 系统内消息中心展示 |

### 7.2 通知配置

每个归档分组可独立配置通知：
- **notifyEnabled**: 是否启用通知
- **notifyUsers**: 接收通知的用户 ID 列表
- **notifyChannels**: 通知渠道列表（FEISHU/WE_COM/IN_APP）
- **feishuWebhook**: 飞书 Webhook URL
- **weComWebhook**: 企业微信 Webhook URL

### 7.3 通知内容

通知消息包含：
- 归档分组名称
- 执行结果（成功/失败）
- 处理记录数
- 执行耗时
- 错误信息（如失败）

### 7.4 API 接口

```
GET  /api/v1/notifications                    # 获取站内消息列表
GET  /api/v1/notifications/unread-count       # 未读消息数
```

---

## 8. 操作审计日志

### 8.1 功能概述

系统自动记录所有关键操作的审计日志，包括创建、更新、删除、状态变更、触发归档、取消任务等。

### 8.2 日志字段

| 字段 | 说明 |
|------|------|
| id | 日志 ID |
| module | 操作模块（数据源/分组/规则/任务/用户等） |
| action | 操作类型（CREATE/UPDATE/DELETE/STATUS/TRIGGER/CANCEL） |
| valueBefore | 操作前值（JSON） |
| valueAfter | 操作后值（JSON） |
| operatorId | 操作人 ID |
| operatorName | 操作人名称 |
| operateTime | 操作时间 |

### 8.3 API 接口

```
GET /api/v1/operation-logs  # 分页查询操作审计日志
```

---

## 9. 用户与权限

### 9.1 角色体系

系统采用 RBAC 模型，内置四种角色：

| 角色编码 | 角色名称 | 数据范围 | 权限说明 |
|----------|---------|---------|---------|
| platform_admin | 平台管理员 | ALL | 拥有全部权限，可管理用户和数据源权限 |
| archive_admin | 归档管理员 | ASSIGNED | 可管理数据源权限、创建普通用户、操作被授权的归档分组 |
| auditor | 审计员 | VIEW | 仅查看权限，无法修改任何配置 |
| observer | 观察员 | VIEW | 仅查看基础信息 |

### 9.2 用户管理

| 操作 | 说明 |
|------|------|
| 创建用户 | 设置用户名、密码、角色、数据源权限 |
| 编辑用户 | 修改角色、数据源权限、启用状态 |
| 删除用户 | 删除用户账号 |
| 修改密码 | 用户自行修改密码 |
| 变更负责人 | 管理员可修改归档分组的负责人 |

### 9.3 安全特性

- JWT 无状态认证
- BCrypt 密码加密
- 会话超时自动登出
- CORS 跨域限制

### 9.4 API 接口

```
POST   /api/v1/auth/login              # 用户登录
POST   /api/v1/auth/logout             # 用户登出
POST   /api/v1/auth/me                 # 获取当前用户信息
POST   /api/v1/auth/change-password    # 修改密码
GET    /api/v1/users                   # 查询用户列表
POST   /api/v1/users                   # 创建用户
PUT    /api/v1/users/{id}              # 更新用户
DELETE /api/v1/users/{id}              # 删除用户
GET    /api/v1/datasource-permissions  # 查询数据源权限
POST   /api/v1/datasource-permissions  # 设置数据源权限
```

---

## 10. 性能与可靠性

### 10.1 并发执行

- 归档分组内规则并行执行
- 单规则内多分片并行处理
- 可配置的线程池参数（核心/最大/队列）

### 10.2 批量处理

- 可配置单次读取行数（默认 5000 条）
- 可配置批次处理间隔（默认 50ms）
- REPLACE INTO 批量写入，减少网络往返

### 10.3 可靠性保障

- **幂等性**：使用 REPLACE INTO 确保重复执行结果一致
- **任务中断恢复**：支持取消正在运行的任务
- **状态机**：完整的任务状态机，防止并发执行
- **终态幂等标记**：成功/失败/取消后状态不再变更

### 10.4 参数调优

```yaml
archive:
  task:
    thread-pool:
      core-pool-size: 10    # 核心线程数
      max-pool-size: 50     # 最大线程数
      queue-capacity: 100   # 队列容量
  rule:
    default-batch-size: 1000    # 默认批次大小
    default-pause-ms: 100       # 批次间停顿(ms)
sync:
  reader:
    load:
      max:
        rows: 5000              # 单次最大加载行数
      unit-time:
        max:
          try:
            frequency: 10000    # 单位时间最大尝试次数
  archive:
    step:
      interval:
        time: 50                # 归档步骤间隔(ms)
```

---

## 11. 技术架构要点

### 11.1 分层架构

```
Vue 3 前端
  │
  ▼
REST API (Spring Boot)
  ├── Controller 层：RESTful 接口 + 操作日志 AOP
  ├── Service 层：业务逻辑 + 权限校验
  └── Repository 层：MyBatis 数据访问
  │
  ▼
核心执行引擎
  ├── ArchiveGroupExecutor：分组并发执行
  ├── ArchiveExecutor：单规则执行
  ├── SyncExecutor：同步执行
  ├── ExpressionEngine：表达式引擎
  └── EventPublisher：事件系统
  │
  ▼
数据持久层
  └── MyBatis + MySQL
```

### 11.2 事件系统

```
TASK_START ──► 分组执行开始
    │
    ▼
RULE_START ──► 单规则开始
    │
    ├── TASK_PROGRESS ──► 进度更新（定期发布）
    │
    ▼
RULE_END ───► 单规则完成
    │
    ▼
TASK_END ───► 分组执行完成
```

事件监听器：
- **DbArchiveLogListener**：持久化归档日志
- **ArchiveNotificationListener**：飞书/企微通知
- **ArchiveInAppNotificationListener**：站内消息

### 11.3 数据库版本管理

使用 Flyway 进行数据库迁移管理，当前包含 12 个迁移脚本，从 V1 到 V12，覆盖用户、数据源、归档分组、规则、任务、通知、权限等所有功能模块。
