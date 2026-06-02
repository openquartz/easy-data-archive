# Archive Execution Progress Update Design

**Date**: 2026-05-31
**Status**: Approved
**Scope**: 实时进度更新（限频写入 DB）

---

## 1. 背景与问题

EasyArchive 归档执行系统当前存在以下进度追踪缺陷：

- `ArchiveExecutor.updateProcess()` 是空 TODO 方法，每次批次后被调用但不执行任何操作
- `TaskEndEvent.totalRows` 始终传 `0L`，实际累加值 `totalProcessRecords` 无法传回 `ArchiveGroupExecutor`
- `ea_archive_group_execute_task` 表的 `processed_records`、`processed_speed`、`heartbeat_time` 字段从未被写入
- 事件系统只有 4 种类型（TASK_START、TASK_END、RULE_START、RULE_END），无 PROGRESS 类型

**目标**：实现归档执行过程中的实时进度更新，支持限频写入数据库，同时修复 totalRows 传递断链。

---

## 2. 设计方案

采用 **ArchiveExecutor 内限频 + 事件系统** 方案。

### 2.1 架构概览

```
ArchiveExecutor.updateProcess()
    |
    +--> 限频判断 (距上次更新 < interval?)
    |       |
    |       +--> 是: return (跳过)
    |       +--> 否: 继续
    |
    +--> 构建 TaskProgressEvent
    |
    +--> publisher.publish(event)
              |
              v
    DbArchiveLogListener.handleTaskProgress()
              |
              +--> UPDATE ea_archive_group_execute_task
              |       SET processed_records, processed_speed, heartbeat_time
              |
              +--> INSERT ea_archive_task_log (logType=PROGRESS)
```

---

## 3. 详细设计

### 3.1 新增 ArchiveEventType 枚举值

**文件**: `easyarchive-core/.../event/ArchiveEventType.java`

```java
public enum ArchiveEventType {
    TASK_START,
    TASK_END,
    TASK_PROGRESS,  // 新增
    RULE_START,
    RULE_END
}
```

### 3.2 新增 TaskProgressEvent 事件类

**文件**: `easyarchive-core/.../event/TaskProgressEvent.java`

```java
public class TaskProgressEvent extends ArchiveEvent {
    private final long processedRecords;   // 累计已处理行数
    private final long elapsedMs;          // 已耗时（毫秒）
    private final Long currentRuleId;      // 当前执行的规则 ID
    private final String sourceTable;      // 当前处理的源表
}
```

继承 `ArchiveEvent`，构造时传入 `ArchiveEventType.TASK_PROGRESS`。

### 3.3 修改 ArchiveConfig

**文件**: `easyarchive-core/.../property/ArchiveConfig.java`

```java
@Value("${sync.archive.progress.update.interval.ms:5000}")
private Long progressUpdateIntervalMs;
```

默认 5 秒更新一次进度。

### 3.4 修改 ArchiveExecutor

**文件**: `easyarchive-core/.../executor/ArchiveExecutor.java`

新增/确认实例字段（`groupId`、`currentRuleId`、`currentSourceTable` 需在 `run()` 方法中循环遍历规则时赋值）：

```java
private final Long groupId;              // 从构造器传入
private Long currentRuleId;              // 遍历规则时赋值: rule.getId()
private String currentSourceTable;       // 遍历规则时赋值: ExpressionService 解析结果
private long lastProgressUpdateTime = 0; // 限频时间戳
```

`run()` 方法中，在每次规则遍历开始时更新 `currentRuleId` 和 `currentSourceTable`：

填充 `updateProcess()` 方法：

```java
private void updateProcess(long totalProcessRecords, long startTime) {
    long now = System.currentTimeMillis();
    if (now - lastProgressUpdateTime < archiveConfig.getProgressUpdateIntervalMs()) {
        return;
    }
    lastProgressUpdateTime = now;
    long elapsedMs = now - startTime;
    publisher.publish(new TaskProgressEvent(
        taskId, groupId, totalProcessRecords, elapsedMs,
        currentRuleId, currentSourceTable
    ));
}
```

新增 getter（用于修复 totalRows 传递）：

```java
public long getTotalProcessRecords() {
    return totalProcessRecords;
}
```

### 3.5 修改 ArchiveGroupExecutor

**文件**: `easyarchive-core/.../ArchiveGroupExecutor.java`

修改 `doExecute()` 方法，获取实际 totalRows：

```java
private void doExecute(List<ArchiveGroupItem> configs) {
    ArchiveExecutor executor = new ArchiveExecutor(...);
    executor.run();
    // 新增：获取实际已处理行数
    // totalRows 通过 executor.getTotalProcessRecords() 获取
}
```

修改 `run()` 中的 `TaskEndEvent` 发布：

```java
// 成功时
publisher.publish(new TaskEndEvent(
    executeTask.getId(), executeTask.getGroupId(),
    true, totalRows, elapsed, null));

// 失败时也传递已处理行数
publisher.publish(new TaskEndEvent(
    executeTask.getId(), executeTask.getGroupId(),
    false, totalRows, elapsed, errorMsg));
```

### 3.6 修改 DbArchiveLogListener

**文件**: `easyarchive-starter/.../listener/DbArchiveLogListener.java`

`onEvent()` 新增 case：

```java
case TASK_PROGRESS:
    handleTaskProgress((TaskProgressEvent) event);
    break;
```

新增 `handleTaskProgress()` 方法：

```java
private void handleTaskProgress(TaskProgressEvent event) {
    ArchiveGroupExecuteTask task = repository.queryTaskById(event.getTaskId());
    if (task == null || task.isTerminal()) {
        return;
    }

    BigDecimal speed = BigDecimal.ZERO;
    if (event.getElapsedMs() > 0) {
        speed = BigDecimal.valueOf(event.getProcessedRecords() * 1000L / event.getElapsedMs())
                .setScale(2, RoundingMode.HALF_UP);
    }

    task.setProcessedRecords(event.getProcessedRecords());
    task.setProcessedSpeed(speed);
    task.setHeartbeatTime(new Date());
    repository.updateTaskExecution(task);

    ArchiveTaskLog log = new ArchiveTaskLog();
    log.setTaskId(event.getTaskId());
    log.setLogType("PROGRESS");
    log.setLogLevel("INFO");
    log.setLogContent(String.format("进度: 已处理 %d 行, 速度 %s 行/秒, 当前表: %s",
            event.getProcessedRecords(), speed, event.getSourceTable()));
    log.setLogTime(new Date());
    log.setProcessedCount(event.getProcessedRecords());
    log.setProcessSpeed(speed);
    log.setExecutePhase("TASK_PROGRESS");
    repository.saveTaskLog(log);
}
```

---

## 4. 数据库更新策略

### 更新字段

| 字段 | 更新时机 | 值来源 |
|------|----------|--------|
| `processed_records` | 进度事件 | `event.getProcessedRecords()` |
| `processed_speed` | 进度事件 | `records * 1000 / elapsedMs` |
| `heartbeat_time` | 进度事件 | `new Date()` |

### 不改动的组件

- `SyncExecutor`：不改动，进度通知在 ArchiveExecutor 层面处理
- `ArchiveLogRepository` 接口：复用现有 `updateTaskExecution()` 和 `saveTaskLog()`
- MyBatis Mapper：`update()` 已支持更新所有字段
- REST API：现有 `/api/v1/task-log/tasks/{taskId}` 已能查询更新后的进度

---

## 5. 配置项

```yaml
sync:
  archive:
    progress:
      update:
        interval:
          ms: 5000  # 默认值，可按需调整
```

---

## 6. 错误处理

1. **任务不存在或已结束**：`handleTaskProgress()` 开头检查 `task.isTerminal()`，跳过更新
2. **DB 更新失败**：`DefaultArchiveEventPublisher` 已有 try-catch 保护单个 listener，不影响其他 listener
3. **进度事件不应阻塞执行**：进度更新失败不影响主流程继续执行

---

## 7. 改动文件清单

| 文件 | 改动类型 | 说明 |
|------|----------|------|
| `ArchiveEventType.java` | 修改 | 新增 `TASK_PROGRESS` |
| `TaskProgressEvent.java` | 新建 | 进度事件类 |
| `ArchiveExecutor.java` | 修改 | 填充 `updateProcess()`，新增字段和 getter |
| `ArchiveGroupExecutor.java` | 修改 | 修复 totalRows 传递 |
| `ArchiveConfig.java` | 修改 | 新增 `progressUpdateIntervalMs` |
| `DbArchiveLogListener.java` | 修改 | 新增 `handleTaskProgress()` |
