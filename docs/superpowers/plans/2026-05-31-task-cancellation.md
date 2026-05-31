# Task Cancellation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add task cancellation support to EasyArchive, allowing users to cancel running archive tasks via API with distributed deployment support via database polling.

**Architecture:** Add `STATUS_CANCELLING(4)` and `STATUS_CANCELLED(5)` to `ArchiveGroupExecuteTask`. The cancel API writes `CANCELLING` status to DB. `ArchiveExecutor.checkCancellation()` polls DB at each checkpoint (rule loop and time/id batch loop), throws `TaskCancelledException` when detected. `ArchiveGroupExecutor` catches it and publishes a `TaskEndEvent` with `cancelled=true`.

**Tech Stack:** Java 11, Spring Boot 2.3.2, MyBatis, Maven

---

### Task 1: Add TaskCancelledException

**Files:**
- Create: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/exception/TaskCancelledException.java`

- [ ] **Step 1: Create TaskCancelledException class**

```java
package com.openquartz.easyarchive.core.exception;

public class TaskCancelledException extends RuntimeException {

    private final Long taskId;

    public TaskCancelledException(Long taskId) {
        super("Task cancelled: " + taskId);
        this.taskId = taskId;
    }

    public TaskCancelledException(Long taskId, String reason) {
        super("Task cancelled: " + taskId + ", reason: " + reason);
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/exception/TaskCancelledException.java
git commit -m "feat: add TaskCancelledException for task cancellation support"
```

---

### Task 2: Add Cancellation Status Constants

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupExecuteTask.java`

- [ ] **Step 1: Add STATUS_CANCELLING and STATUS_CANCELLED constants**

Add after line 22 (`STATUS_FAILED = 3;`):

```java
    public static final int STATUS_CANCELLING = 4;
    public static final int STATUS_CANCELLED = 5;
```

Update the class-level javadoc comment to:

```java
/**
 * 归档执行任务
 *
 * execute_status: 0-等待 1-运行中 2-成功 3-失败 4-取消中 5-已取消
 */
```

Update the field-level javadoc for `executeStatus` to:

```java
    /**
     * 执行状态: 0-等待 1-运行中 2-成功 3-失败 4-取消中 5-已取消
     */
    private Integer executeStatus;
```

- [ ] **Step 2: Update isTerminal() to include STATUS_CANCELLED**

Replace the `isTerminal()` method (lines 73-76) with:

```java
    public boolean isTerminal() {
        return executeStatus != null
                && (executeStatus == STATUS_SUCCESS
                    || executeStatus == STATUS_FAILED
                    || executeStatus == STATUS_CANCELLED);
    }
```

- [ ] **Step 3: Compile to verify**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupExecuteTask.java
git commit -m "feat: add STATUS_CANCELLING and STATUS_CANCELLED constants"
```

---

### Task 3: Add cancelled Flag to TaskEndEvent

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskEndEvent.java`

- [ ] **Step 1: Add cancelled field and update constructor**

Replace the entire file with:

```java
package com.openquartz.easyarchive.core.event;

public class TaskEndEvent extends ArchiveEvent {

    private final boolean success;
    private final long totalRows;
    private final long elapsedMs;
    private final String errorMsg;
    private final boolean cancelled;

    public TaskEndEvent(Long taskId, Long groupId,
                        boolean success, long totalRows,
                        long elapsedMs, String errorMsg) {
        this(taskId, groupId, success, totalRows, elapsedMs, errorMsg, false);
    }

    public TaskEndEvent(Long taskId, Long groupId,
                        boolean success, long totalRows,
                        long elapsedMs, String errorMsg, boolean cancelled) {
        super(ArchiveEventType.TASK_END, taskId, groupId);
        this.success = success;
        this.totalRows = totalRows;
        this.elapsedMs = elapsedMs;
        this.errorMsg = errorMsg;
        this.cancelled = cancelled;
    }

    public boolean isSuccess() { return success; }
    public long getTotalRows() { return totalRows; }
    public long getElapsedMs() { return elapsedMs; }
    public String getErrorMsg() { return errorMsg; }
    public boolean isCancelled() { return cancelled; }
}
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/event/TaskEndEvent.java
git commit -m "feat: add cancelled flag to TaskEndEvent"
```

---

### Task 4: Add updateTaskStatus to Repository

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/repository/ArchiveLogRepository.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/repository/JdbcArchiveLogRepository.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`

- [ ] **Step 1: Add method to ArchiveLogRepository interface**

Add after `countLogsByTaskId` method (before `deleteByRetentionDays`):

```java
    void updateTaskStatus(Long taskId, int status);
```

- [ ] **Step 2: Add method to ArchiveGroupExecuteTaskMapper**

Add after `count` method:

```java
    int updateExecuteStatus(@Param("id") Long id, @Param("status") Integer status);
```

- [ ] **Step 3: Add SQL mapping in ArchiveGroupExecuteTaskMapper.xml**

Add before the closing `</mapper>` tag:

```xml
    <update id="updateExecuteStatus">
        UPDATE ea_archive_group_execute_task
        SET execute_status = #{status}, updated_time = NOW()
        WHERE id = #{id} AND deleted = 0
    </update>
```

- [ ] **Step 4: Implement in JdbcArchiveLogRepository**

Add after the `countLogsByTaskId` method (before `deleteByRetentionDays`):

```java
    @Override
    public void updateTaskStatus(Long taskId, int status) {
        executeTaskMapper.updateExecuteStatus(taskId, status);
    }
```

- [ ] **Step 5: Compile to verify**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/repository/ArchiveLogRepository.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java \
        easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/repository/JdbcArchiveLogRepository.java
git commit -m "feat: add updateTaskStatus to repository layer"
```

---

### Task 5: Implement checkCancellation in ArchiveExecutor

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java`

- [ ] **Step 1: Add import for TaskCancelledException and ArchiveLogRepository**

Add these imports after the existing imports:

```java
import com.openquartz.easyarchive.core.exception.TaskCancelledException;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
```

- [ ] **Step 2: Add repository field and update constructor**

Add a new field after the `groupId` field:

```java
    private final ArchiveLogRepository archiveLogRepository;
```

Update the constructor to accept and store the repository. Replace the constructor (lines 49-63) with:

```java
    public ArchiveExecutor(ArchiveConnection sourceConnection,
                           ArchiveConnection sinkConnection,
                           ArchiveConfig archiveConfig,
                           List<ArchiveGroupItem> ruleList,
                           Long taskId,
                           Long groupId,
                           ArchiveEventPublisher publisher,
                           ArchiveLogRepository archiveLogRepository) {
        this.sourceConnection = sourceConnection;
        this.sinkConnection = sinkConnection;
        this.archiveConfig = archiveConfig;
        this.ruleList = ruleList;
        this.taskId = taskId;
        this.groupId = groupId;
        this.publisher = publisher;
        this.archiveLogRepository = archiveLogRepository;
    }
```

- [ ] **Step 3: Implement checkCancellation()**

Replace the `checkCancellation()` method (lines 199-201) with:

```java
    private void checkCancellation() {
        try {
            ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(taskId);
            if (task != null && task.getExecuteStatus() != null
                    && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_CANCELLING) {
                throw new TaskCancelledException(taskId);
            }
        } catch (TaskCancelledException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[ArchiveExecutor] Failed to check cancellation status for task {}: {}",
                    taskId, e.getMessage());
        }
    }
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java
git commit -m "feat: implement checkCancellation with DB polling"
```

---

### Task 6: Update ArchiveGroupExecutor to Handle Cancellation

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java`

- [ ] **Step 1: Add imports**

Add after existing imports:

```java
import com.openquartz.easyarchive.core.exception.TaskCancelledException;
import com.openquartz.easyarchive.core.repository.ArchiveLogRepository;
```

- [ ] **Step 2: Add repository field and update constructor**

Add field after `publisher`:

```java
    private final ArchiveLogRepository archiveLogRepository;
```

Update the constructor to accept and store the repository. Replace the constructor (lines 28-38) with:

```java
    public ArchiveGroupExecutor(ArchiveRuleLoader loader,
                                ArchiveConfig archiveConfig,
                                ArchiveGroupExecuteTask executeTask,
                                Pair<ArchiveConnection, ArchiveConnection> connectionInfo,
                                ArchiveEventPublisher publisher,
                                ArchiveLogRepository archiveLogRepository) {
        this.loader = loader;
        this.archiveConfig = archiveConfig;
        this.executeTask = executeTask;
        this.connectionInfo = connectionInfo;
        this.publisher = publisher;
        this.archiveLogRepository = archiveLogRepository;
    }
```

- [ ] **Step 3: Update doExecute to pass repository to ArchiveExecutor**

Replace the `doExecute` method (lines 77-82) with:

```java
    private long doExecute(List<ArchiveGroupItem> configs) {
        ArchiveExecutor executor = new ArchiveExecutor(connectionInfo.getKey(), connectionInfo.getValue(),
            archiveConfig, configs, executeTask.getId(), executeTask.getGroupId(), publisher, archiveLogRepository);
        executor.run();
        return executor.getTotalProcessRecords();
    }
```

- [ ] **Step 4: Add TaskCancelledException handling in run()**

Replace the catch block in `run()` (lines 65-74) with:

```java
        } catch (TaskCancelledException ex) {
            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                false, 0L, elapsed, "Task cancelled", true));

            log.info("[ArchiveGroupExecutor#run] archive cancelled, taskId:{}", executeTask.getId());

        } catch (Exception ex) {
            long elapsed = System.currentTimeMillis() - startMs;

            publisher.publish(new TaskEndEvent(
                executeTask.getId(), executeTask.getGroupId(),
                false, 0L, elapsed, ex.getMessage()));

            log.error("[ArchiveGroupExecutor#run] archive failed, taskId:{}", executeTask.getId(), ex);
            ExceptionUtils.rethrow(ex);
        }
```

- [ ] **Step 5: Compile to verify**

Run: `mvn compile -pl easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/ArchiveGroupExecutor.java
git commit -m "feat: handle TaskCancelledException in ArchiveGroupExecutor"
```

---

### Task 7: Update DbArchiveLogListener for Cancellation

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java`

- [ ] **Step 1: Update handleTaskEnd to support cancelled state**

Replace the `handleTaskEnd` method (lines 55-77) with:

```java
    private void handleTaskEnd(TaskEndEvent event) {
        ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
        task.setId(event.getTaskId());
        task.setEndTime(new Date(event.getTimestamp()));
        task.setProcessedRecords(event.getTotalRows());

        String content;
        String logType;
        String level;

        if (event.isCancelled()) {
            task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLED);
            task.setFinishedFlag(0L);
            content = "任务已取消" + (event.getErrorMsg() != null ? ":" + event.getErrorMsg() : "");
            logType = "CANCEL";
            level = "WARN";
        } else {
            task.setExecuteStatus(event.isSuccess() ? 2 : 3);
            task.setFinishedFlag(event.isSuccess() ? event.getTaskId() : 0L);
            if (event.getElapsedMs() > 0) {
                task.setProcessedSpeed(BigDecimal.valueOf(event.getTotalRows() * 1000.0 / event.getElapsedMs()));
            }
            if (!event.isSuccess() && event.getErrorMsg() != null) {
                task.setErrorMsg(event.getErrorMsg());
            }
            content = event.isSuccess()
                    ? "任务完成，总行数:" + event.getTotalRows() + "，耗时:" + event.getElapsedMs() + "ms"
                    : "任务失败:" + event.getErrorMsg();
            logType = event.isSuccess() ? "FINISH" : "ERROR";
            level = event.isSuccess() ? "INFO" : "ERROR";
        }

        repository.updateTaskExecution(task);
        saveLog(event.getTaskId(), logType, level, content,
                "TASK_END", event.getTotalRows(), BigDecimal.ZERO, new Date(event.getTimestamp()));
    }
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java
git commit -m "feat: handle cancelled state in DbArchiveLogListener"
```

---

### Task 8: Add Cancel API Endpoint

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskLogService.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java`

- [ ] **Step 1: Add cancelTask method to service interface**

Add after the `cleanup` method in `ArchiveTaskLogService.java`:

```java
    void cancelTask(Long taskId, String cancelReason);
```

- [ ] **Step 2: Implement cancelTask in service**

Add after the `cleanup` method in `ArchiveTaskLogServiceImpl.java`. Also add these imports at the top:

```java
import com.openquartz.easyarchive.core.rule.entity.ArchiveGroupExecuteTask;
import java.math.BigDecimal;
import java.util.Date;
```

Then add the method:

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTask(Long taskId, String cancelReason) {
        ArchiveGroupExecuteTask task = archiveLogRepository.queryTaskById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在: " + taskId);
        }
        if (task.isTerminal()) {
            throw new IllegalStateException("任务已结束，无法取消");
        }
        if (task.getExecuteStatus() != null
                && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_CANCELLING) {
            return; // already cancelling, idempotent
        }
        if (task.getExecuteStatus() != null
                && task.getExecuteStatus() == ArchiveGroupExecuteTask.STATUS_WAITING) {
            archiveLogRepository.updateTaskStatus(taskId, ArchiveGroupExecuteTask.STATUS_CANCELLED);
        } else {
            archiveLogRepository.updateTaskStatus(taskId, ArchiveGroupExecuteTask.STATUS_CANCELLING);
        }

        ArchiveTaskLog log = new ArchiveTaskLog();
        log.setTaskId(taskId);
        log.setLogType("CANCEL");
        log.setLogLevel("WARN");
        log.setLogContent("任务取消请求" + (cancelReason != null ? ":" + cancelReason : ""));
        log.setExecutePhase("TASK_END");
        log.setProcessedCount(task.getProcessedRecords() != null ? task.getProcessedRecords() : 0L);
        log.setProcessSpeed(BigDecimal.ZERO);
        log.setLogTime(new Date());
        archiveLogRepository.saveTaskLog(log);
    }
```

Note: Remove the duplicate import of `ArchiveGroupExecuteTask` if it already exists (it does not currently exist in this file).

- [ ] **Step 3: Add cancel endpoint to controller**

Add after the `cleanup` method in `ArchiveTaskLogController.java`. Add these imports at the top:

```java
import java.util.Collections;
```

Then add the endpoint:

```java
    @PostMapping("/tasks/{taskId}/cancel")
    public ApiResponse<Object> cancelTask(
            @PathVariable Long taskId,
            @RequestBody(required = false) Map<String, String> body) {
        try {
            String reason = body != null ? body.get("cancelReason") : null;
            taskLogService.cancelTask(taskId, reason);
            return ApiResponse.success("取消请求已提交");
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("NOT_FOUND", e.getMessage());
        } catch (IllegalStateException e) {
            return ApiResponse.error("INVALID_STATUS", e.getMessage());
        }
    }
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskLogService.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java
git commit -m "feat: add POST /api/v1/task-log/tasks/{taskId}/cancel endpoint"
```

---

### Task 9: Full Build Verification

**Files:** None (verification only)

- [ ] **Step 1: Build entire project**

Run: `mvn clean compile -DskipTests -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run tests**

Run: `mvn test -q`
Expected: BUILD SUCCESS (no test failures)

- [ ] **Step 3: Final commit (if any fixups needed)**

If any fixups were needed during build verification, commit them:

```bash
git add -A
git commit -m "fix: address build issues from task cancellation implementation"
```
