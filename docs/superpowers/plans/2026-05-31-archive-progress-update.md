# Archive Execution Progress Update Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现归档执行过程中的实时进度更新，限频写入数据库，修复 totalRows 传递断链。

**Architecture:** 新增 `TASK_PROGRESS` 事件类型和 `TaskProgressEvent` 事件类，在 `ArchiveExecutor.updateProcess()` 中实现限频逻辑并通过事件系统发布进度。`DbArchiveLogListener` 处理进度事件更新 `ea_archive_group_execute_task` 表。

**Tech Stack:** Java 11, Spring Boot 2.3.2, Maven, JUnit 5, Mockito

---

### Task 1: 新增 TASK_PROGRESS 枚举值

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEventType.java`

- [ ] **Step 1: 在 ArchiveEventType 中添加 TASK_PROGRESS**

```java
package com.openquartz.easyarchive.core.event;

public enum ArchiveEventType {
    TASK_START,
    TASK_END,
    TASK_PROGRESS,
    RULE_START,
    RULE_END
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/ArchiveEventType.java
git commit -m "feat: add TASK_PROGRESS event type"
```

---

### Task 2: 新增 TaskProgressEvent 事件类

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskProgressEvent.java`

- [ ] **Step 1: 创建 TaskProgressEvent 类**

参照 `TaskStartEvent`（`easyarchive-core/.../event/TaskStartEvent.java`）的结构：

```java
package com.openquartz.easyarchive.core.event;

public class TaskProgressEvent extends ArchiveEvent {

    private final long processedRecords;
    private final long elapsedMs;
    private final Long currentRuleId;
    private final String sourceTable;

    public TaskProgressEvent(Long taskId, Long groupId,
                             long processedRecords, long elapsedMs,
                             Long currentRuleId, String sourceTable) {
        super(ArchiveEventType.TASK_PROGRESS, taskId, groupId);
        this.processedRecords = processedRecords;
        this.elapsedMs = elapsedMs;
        this.currentRuleId = currentRuleId;
        this.sourceTable = sourceTable;
    }

    public long getProcessedRecords() { return processedRecords; }
    public long getElapsedMs() { return elapsedMs; }
    public Long getCurrentRuleId() { return currentRuleId; }
    public String getSourceTable() { return sourceTable; }
}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskProgressEvent.java
git commit -m "feat: add TaskProgressEvent event class"
```

---

### Task 3: 新增进度更新间隔配置

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/property/ArchiveConfig.java:61`

- [ ] **Step 1: 在 logRetentionDays 字段后面添加新配置**

```java
    /**
     * 日志保留天数
     */
    @Value("${sync.log.retention-days:30}")
    private int logRetentionDays;

    /**
     * 进度更新间隔(毫秒)
     */
    @Value("${sync.archive.progress.update.interval.ms:5000}")
    private Long progressUpdateIntervalMs;

}
```

- [ ] **Step 2: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/property/ArchiveConfig.java
git commit -m "feat: add progressUpdateIntervalMs config"
```

---

### Task 4: 填充 ArchiveExecutor.updateProcess() 并修复 totalRows

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java`

- [ ] **Step 1: 添加新字段和 import**

在 `ArchiveExecutor.java` 的字段声明区域（第 34-39 行之后）添加：

```java
    private final Long groupId;

    private Long currentRuleId;
    private String currentSourceTable;
    private long totalProcessRecords;
    private long lastProgressUpdateTime = 0;
```

修改构造器（第 41-53 行），增加 `groupId` 参数并初始化新字段：

```java
    public ArchiveExecutor(ArchiveConnection sourceConnection,
                           ArchiveConnection sinkConnection,
                           ArchiveConfig archiveConfig,
                           List<ArchiveGroupItem> ruleList,
                           Long taskId,
                           Long groupId,
                           ArchiveEventPublisher publisher) {
        this.sourceConnection = sourceConnection;
        this.sinkConnection = sinkConnection;
        this.archiveConfig = archiveConfig;
        this.ruleList = ruleList;
        this.taskId = taskId;
        this.groupId = groupId;
        this.publisher = publisher;
    }
```

添加 import：

```java
import com.openquartz.easyarchive.core.event.TaskProgressEvent;
```

- [ ] **Step 2: 修改 run() 方法中的变量**

将 `run()` 方法中的局部变量 `long totalProcessRecords = 0;`（第 58 行）删除，因为已改为实例字段。

在每次规则遍历开始时（第 66 行之后）设置当前规则信息：

```java
            String ruleType = (rule instanceof ArchiveGroupItemByTime) ? "TIME" : "ID";
            this.currentRuleId = rule.getId();
            this.currentSourceTable = ExpressionService.getInstance().parse(rule.getSourceTable());
```

- [ ] **Step 3: 填充 updateProcess() 方法**

替换第 156-158 行的空实现：

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

- [ ] **Step 4: 添加 getter 方法**

在 `checkCancellation()` 方法之前添加：

```java
    public long getTotalProcessRecords() {
        return totalProcessRecords;
    }
```

- [ ] **Step 5: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS（此时 ArchiveGroupExecutor 会编译失败，因为构造器签名变了，这是预期的）

- [ ] **Step 6: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java
git commit -m "feat: implement updateProcess with rate-limited progress events"
```

---

### Task 5: 修复 ArchiveGroupExecutor 的 totalRows 传递

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java`

- [ ] **Step 1: 修改 doExecute() 方法获取实际 totalRows**

替换第 77-80 行：

```java
    private long doExecute(List<ArchiveGroupItem> configs) {
        ArchiveExecutor executor = new ArchiveExecutor(connectionInfo.getKey(), connectionInfo.getValue(),
            archiveConfig, configs, executeTask.getId(), executeTask.getGroupId(), publisher);
        executor.run();
        return executor.getTotalProcessRecords();
    }
```

- [ ] **Step 2: 修改 run() 方法使用实际 totalRows**

替换第 54-60 行的成功路径：

```java
            long totalRows = doExecute(configs);

            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                true, totalRows, elapsed, null));
```

替换第 65-70 行的失败路径（需要在 catch 前声明变量）：

```java
        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                false, 0L, elapsed, ex.getMessage()));
```

注意：失败路径中 totalRows 传 `0L` 是合理的，因为异常时无法获取 executor 的内部状态。

- [ ] **Step 3: 编译验证**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java
git commit -m "feat: pass actual totalRows in TaskEndEvent"
```

---

### Task 6: 在 DbArchiveLogListener 中处理 TASK_PROGRESS 事件

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java`

- [ ] **Step 1: 添加 import**

```java
import com.openquartz.easyarchive.core.event.TaskProgressEvent;
import java.math.RoundingMode;
```

- [ ] **Step 2: 在 onEvent() 中添加 TASK_PROGRESS 分支**

在第 33 行 `handleRuleEnd` 分支之后添加：

```java
        } else if (event instanceof TaskProgressEvent) {
            handleTaskProgress((TaskProgressEvent) event);
        }
```

- [ ] **Step 3: 添加 handleTaskProgress() 方法**

在 `handleRuleEnd()` 方法之后、`saveLog()` 方法之前添加：

```java
    private void handleTaskProgress(TaskProgressEvent event) {
        ArchiveGroupExecuteTask task = repository.queryTaskById(event.getTaskId());
        if (task == null || task.isTerminal()) {
            return;
        }

        BigDecimal speed = BigDecimal.ZERO;
        if (event.getElapsedMs() > 0) {
            speed = BigDecimal.valueOf(event.getProcessedRecords() * 1000.0 / event.getElapsedMs())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        task.setProcessedRecords(event.getProcessedRecords());
        task.setProcessedSpeed(speed);
        task.setHeartbeatTime(new Date());
        repository.updateTaskExecution(task);

        String content = String.format("进度: 已处理 %d 行, 速度 %s 行/秒, 当前表: %s",
                event.getProcessedRecords(), speed, event.getSourceTable());
        saveLog(event.getTaskId(), "PROGRESS", "INFO", content,
                "TASK_PROGRESS", event.getProcessedRecords(), speed, new Date(event.getTimestamp()));
    }
```

- [ ] **Step 4: 编译验证**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java
git commit -m "feat: handle TASK_PROGRESS event in DbArchiveLogListener"
```

---

### Task 7: 全量编译验证

**Files:** 无新增/修改

- [ ] **Step 1: 全量编译**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 运行现有测试**

Run: `mvn test -q`
Expected: BUILD SUCCESS（无失败测试）

- [ ] **Step 3: 最终 Commit（如有遗漏文件）**

```bash
git status
# 确认所有改动已提交
```
