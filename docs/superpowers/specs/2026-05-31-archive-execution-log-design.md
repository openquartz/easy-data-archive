# 归档任务执行日志功能设计文档

> 日期: 2026-05-31
> 模块: easyarchive-core / easyarchive-starter
> 参考: xxl-job 执行日志记录机制

## 1. 背景与目标

### 现状

- `ArchiveGroupExecutor`、`ArchiveExecutor` 中存在大量 `// TODO 记录执行日志` 占位符，日志记录未实现
- 已预定义 `ArchiveTaskLog`、`ArchiveGroupExecuteTask`、`ArchiveStatistics` 等实体，但均未被使用
- `ArchiveExecutor:145` 日志消息有 bug（写了 "execute error!" 但实际是正常完成）
- `doReportExecuteProcess()`、`checkCancellation()`、`updateProcess()` 均为空实现

### 目标

设计并实现归档任务执行日志功能，具备以下能力：

1. 在执行流程关键节点记录结构化执行日志到数据库
2. 支持两层日志粒度：任务级 + 规则级
3. 提供 REST API 查询日志
4. 支持可配置的日志保留天数和自动清理
5. 高扩展性，易于新增日志输出方式

## 2. 设计方案：事件驱动模式（Observer/Publisher Pattern）

### 架构总览

```
┌─────────────────────────────────────────────────────────┐
│                    Execution Layer                       │
│  ArchiveGroupExecutor ──publish──> ArchiveEventPublisher │
│  ArchiveExecutor      ──publish──>       │               │
└─────────────────────────────────────────┼───────────────┘
                                          │
                              ┌───────────┼───────────┐
                              ▼           ▼           ▼
                        DbLogListener  Slf4jListener  NoOpListener
                              │
                              ▼
                     ArchiveLogRepository → Database
                              │
                              ▼
                   ArchiveTaskLogService → REST API
```

### 设计原则

- 执行逻辑与日志持久化完全解耦
- 事件对象不可变，线程安全
- 日志关闭时使用 NoOp 实现，零开销
- 不强制依赖 Spring 容器

## 3. 事件模型（Event Model）

### 事件类型

```java
public enum ArchiveEventType {
    TASK_START,    // 任务开始
    TASK_END,      // 任务结束
    RULE_START,    // 规则开始
    RULE_END       // 规则结束
}
```

### 事件类层次

```java
// 事件基类
public class ArchiveEvent {
    private final String eventId;          // UUID
    private final ArchiveEventType type;
    private final Long taskId;             // ArchiveGroupExecuteTask.id
    private final Long groupId;            // ArchiveGroup.id
    private final long timestamp;          // System.currentTimeMillis()
}

// 任务级事件
public class TaskStartEvent extends ArchiveEvent {
    private final int ruleCount;
}

public class TaskEndEvent extends ArchiveEvent {
    private final boolean success;
    private final long totalRows;
    private final long elapsedMs;
    private final String errorMsg;
}

// 规则级事件
public class RuleStartEvent extends ArchiveEvent {
    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;         // "TIME" | "ID"
}

public class RuleEndEvent extends ArchiveEvent {
    private final String sourceTable;
    private final String targetTable;
    private final String ruleType;
    private final boolean success;
    private final long processedRows;
    private final long elapsedMs;
    private final String errorMsg;
}
```

### 设计要点

- 所有字段 `final`，事件对象不可变，线程安全
- `eventId` 使用 UUID，可用于幂等去重
- 事件携带完整上下文，Listener 无需回查执行状态
- 事件生成是纯内存操作，不阻塞执行流程

## 4. 事件发布器与监听器（Publisher & Listener）

### 监听器接口

```java
public interface ArchiveEventListener {
    void onEvent(ArchiveEvent event);
}
```

### 发布器接口

```java
public interface ArchiveEventPublisher {
    void publish(ArchiveEvent event);
    void registerListener(ArchiveEventListener listener);
}
```

### 默认实现

```java
public class DefaultArchiveEventPublisher implements ArchiveEventPublisher {

    private final List<ArchiveEventListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void publish(ArchiveEvent event) {
        for (ArchiveEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("[ArchiveEventPublisher] listener error", e);
            }
        }
    }

    @Override
    public void registerListener(ArchiveEventListener listener) {
        listeners.add(listener);
    }
}
```

### 空实现（日志关闭时使用）

```java
public class NoOpArchiveEventPublisher implements ArchiveEventPublisher {
    @Override
    public void publish(ArchiveEvent event) { /* no-op */ }
    @Override
    public void registerListener(ArchiveEventListener listener) { /* no-op */ }
}
```

### 设计要点

- `CopyOnWriteArrayList` 保证线程安全
- 单个 Listener 异常不影响其他 Listener 和执行流程
- 当 `sync.log.enabled=false` 时使用 `NoOpArchiveEventPublisher`

## 5. 日志持久化

### 日志仓储接口

```java
public interface ArchiveLogRepository {
    void saveTaskExecution(ArchiveGroupExecuteTask task);
    void updateTaskExecution(ArchiveGroupExecuteTask task);
    void saveTaskLog(ArchiveTaskLog log);
    List<ArchiveGroupExecuteTask> queryTasks(int page, int size, Integer status);
    ArchiveGroupExecuteTask queryTaskById(Long taskId);
    List<ArchiveTaskLog> queryLogsByTaskId(Long taskId, int page, int size, String executePhase);
    int deleteByRetentionDays(int retentionDays);
}
```

### 数据库监听器

```java
public class DbArchiveLogListener implements ArchiveEventListener {

    private final ArchiveLogRepository repository;

    @Override
    public void onEvent(ArchiveEvent event) {
        if (event instanceof TaskStartEvent) {
            handleTaskStart((TaskStartEvent) event);
        } else if (event instanceof TaskEndEvent) {
            handleTaskEnd((TaskEndEvent) event);
        } else if (event instanceof RuleStartEvent) {
            handleRuleStart((RuleStartEvent) event);
        } else if (event instanceof RuleEndEvent) {
            handleRuleEnd((RuleEndEvent) event);
        }
    }
}
```

### ArchiveTaskLog 字段映射

| 字段 | TASK_START | TASK_END | RULE_START | RULE_END |
|---|---|---|---|---|
| `taskId` | executeTask.id | executeTask.id | executeTask.id | executeTask.id |
| `logType` | 1 | 2 | 3 | 4 |
| `logLevel` | INFO | INFO / ERROR | INFO | INFO / ERROR |
| `logContent` | "任务开始，规则数:N" | "任务结束/失败" | "规则开始:src->dst" | "规则结束:处理N行" |
| `executePhase` | TASK_START | TASK_END | RULE_START | RULE_END |
| `processedCount` | 0 | 总行数 | 0 | 本规则行数 |
| `processSpeed` | 0 | 行/秒 | 0 | 行/秒 |

### ArchiveGroupExecuteTask 更新时机

- **TASK_START**: 创建记录，设置 `startTime`、`executeStatus=RUNNING`
- **TASK_END**: 更新 `endTime`、`executeStatus`（SUCCESS/FAILED）、`processedRecords`、`processedSpeed`、`errorMsg`、`finishedFlag`

## 6. 执行器集成

### ArchiveGroupExecutor 改造

新增 `ArchiveEventPublisher` 构造参数。在以下节点发布事件：

| 节点 | 事件 | 说明 |
|---|---|---|
| `run()` 入口 | `TaskStartEvent` | 替换原有空 catch 块中的 TODO |
| `run()` 正常结束 | `TaskEndEvent(success=true)` | 替换 `doReportExecuteProcess()` |
| `run()` 异常捕获 | `TaskEndEvent(success=false)` | 替换原有空 catch 块 |

### ArchiveExecutor 改造

新增 `ArchiveEventPublisher` 构造参数。在以下节点发布事件：

| 节点 | 事件 | 说明 |
|---|---|---|
| for 循环内，规则开始前 | `RuleStartEvent` | 替换 `// TODO 开始打印日志` |
| 规则正常结束 | `RuleEndEvent(success=true)` | 替换 `// TODO 记录执行日志`，修正 "execute error!" bug |
| 规则异常 | `RuleEndEvent(success=false)` | 替换 `// TODO 拼接执行日志` |

### 改造原则

- 只插入 `publisher.publish()` 调用，不改变原有业务逻辑
- 替换所有 `// TODO 记录执行日志` 占位符
- 修正 `ArchiveExecutor:145` 的 bug

## 7. REST API

### 接口定义

```
GET  /api/task-log/tasks?page=1&size=20&status=1
     → 分页查询任务执行列表

GET  /api/task-log/tasks/{taskId}
     → 查询单个任务详情

GET  /api/task-log/tasks/{taskId}/logs?page=1&size=50&executePhase=RULE_END
     → 查询任务下的执行日志

POST /api/task-log/cleanup?retentionDays=30
     → 手动触发日志清理
```

### 组件分层

```
ArchiveTaskLogController    ← REST 入口
    ↓
ArchiveTaskLogService       ← 业务逻辑
    ↓
ArchiveLogRepository        ← 数据访问
```

### 响应格式

统一使用项目已有的 `ApiResponse<T>` 包装。

## 8. 自动清理

### 定时任务

```java
@Component
public class ArchiveLogCleanupTask {

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanup() {
        int retentionDays = archiveConfig.getLogRetentionDays();
        int deleted = repository.deleteByRetentionDays(retentionDays);
        log.info("[ArchiveLogCleanup] cleaned {} records older than {} days",
                 deleted, retentionDays);
    }
}
```

### 配置项

| 配置项 | 属性名 | 默认值 | 说明 |
|---|---|---|---|
| `sync.log.enabled` | logEnabled | true | 是否启用执行日志 |
| `sync.log.retention-days` | logRetentionDays | 30 | 日志保留天数 |

## 9. 组件清单

### easyarchive-core 模块（新增）

| 包路径 | 类名 | 职责 |
|---|---|---|
| `core.event` | `ArchiveEventType` | 事件类型枚举 |
| `core.event` | `ArchiveEvent` | 事件基类 |
| `core.event` | `TaskStartEvent` | 任务开始事件 |
| `core.event` | `TaskEndEvent` | 任务结束事件 |
| `core.event` | `RuleStartEvent` | 规则开始事件 |
| `core.event` | `RuleEndEvent` | 规则结束事件 |
| `core.event` | `ArchiveEventPublisher` | 发布器接口 |
| `core.event` | `DefaultArchiveEventPublisher` | 默认发布器实现 |
| `core.event` | `NoOpArchiveEventPublisher` | 空发布器（日志关闭） |
| `core.listener` | `ArchiveEventListener` | 监听器接口 |
| `core.listener` | `DbArchiveLogListener` | 数据库日志监听器 |
| `core.repository` | `ArchiveLogRepository` | 日志仓储接口 |

### easyarchive-starter 模块（新增）

| 包路径 | 类名 | 职责 |
|---|---|---|
| `starter.repository` | `JdbcArchiveLogRepository` | JDBC 日志仓储实现 |
| `starter.service` | `ArchiveTaskLogService` | 日志查询服务 |
| `starter.controller` | `ArchiveTaskLogController` | REST API 控制器 |
| `starter.task` | `ArchiveLogCleanupTask` | 定时清理任务 |

### 现有文件修改

| 文件 | 改动 |
|---|---|
| `ArchiveGroupExecutor.java` | 新增 publisher 字段，发布 TaskStart/End 事件 |
| `ArchiveExecutor.java` | 新增 publisher 字段，发布 RuleStart/End 事件，修正日志 bug |
| `ArchiveConfig.java` | 新增 logEnabled、logRetentionDays 配置 |
| `ArchiveGroupExecuteTask.java` | 实现 wrapFinishedFlag() |
| `ArchiveTaskLog.java` | 无需修改，直接复用 |

## 10. 建表 SQL

```sql
CREATE TABLE IF NOT EXISTS ea_archive_task_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id         BIGINT       NOT NULL COMMENT '关联 ea_archive_group_execute_task.id',
    log_type        INT          NOT NULL COMMENT '日志类型: 1-TASK_START,2-TASK_END,3-RULE_START,4-RULE_END',
    log_level       VARCHAR(10)  NOT NULL COMMENT '日志级别: INFO/ERROR',
    log_content     TEXT         NOT NULL COMMENT '日志内容',
    log_time        DATETIME     NOT NULL COMMENT '日志时间',
    processed_count BIGINT       DEFAULT 0 COMMENT '已处理记录数',
    process_speed   BIGINT       DEFAULT 0 COMMENT '处理速度(记录/秒)',
    execute_phase   VARCHAR(20)  NOT NULL COMMENT '执行阶段: TASK_START/TASK_END/RULE_START/RULE_END',
    created_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    creator_id      VARCHAR(64),
    updater_id      VARCHAR(64),
    deleted         BIGINT       DEFAULT 0,
    INDEX idx_task_id (task_id),
    INDEX idx_log_time (log_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档任务执行日志';
```

注：`ea_archive_group_execute_task` 表结构参考 `ArchiveGroupExecuteTask` 实体字段建表，此处不再赘述。
