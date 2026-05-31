# EasyArchive Task Cancellation Design

## Overview

Add task cancellation support to EasyArchive's archive execution engine. Users can cancel running archive tasks via API. Cancelled tasks stop gracefully at the next checkpoint without rolling back already-migrated data. Distributed deployment is supported via database polling.

## Requirements

- **Task-level granularity**: Cancel an entire archive task (all rules stop)
- **Distributed coordination**: Database polling (reuse progress update interval, default 5s)
- **Data consistency**: Stop immediately, no rollback of migrated data
- **Check interval**: Reuse `progressUpdateIntervalMs` (no new config)

## State Machine

```
                    +-------------+
                    |  WAITING(0) |
                    +------+------+
                           | task starts
                           v
                    +-------------+
            +-------|  RUNNING(1) |-------+
            |       +------+------       |
            |              |              |
     cancel request   normal complete   exception
            |              |              |
            v              v              v
   +--------------+ +----------+ +----------+
   |CANCELLING(4) | |SUCCESS(2)| | FAILED(3)|
   +------+-------+ +----------+ +----------+
          | detects cancel flag
          v
   +--------------+
   |CANCELLED(5)  |
   +--------------+
```

## Cancellation Flow

```
User Node                  DB                  Executor Node
   |                        |                      |
   | POST /cancel/{taskId}  |                      |
   |----------------------->|                      |
   | UPDATE status=4        |                      |
   | (CANCELLING)           |                      |
   |                        |                      |
   |                        |   <- poll (every 5s)  |
   |                        |   status=4? YES       |
   |                        |                      |
   |                        |   TaskCancelledException
   |                        |                      |
   |                        |   UPDATE status=5    |
   |                        |   (CANCELLED)        |
   |                        |   <----------------- |
   |                        |                      |
   |                        |   publish TaskEndEvent
```

## Component Changes

### 1. TaskCancelledException (new)

Package: `com.openquartz.easyarchive.core.exception`

- Extends `RuntimeException`
- Fields: `taskId` (Long), `reason` (String)
- Thrown at checkpoints when cancellation is detected

### 2. ArchiveGroupExecuteTask Status Constants

Add two new status constants:

```java
public static final int STATUS_CANCELLING = 4;
public static final int STATUS_CANCELLED = 5;
```

Update `isTerminal()` to include CANCELLED:

```java
public boolean isTerminal() {
    return executeStatus != null
            && (executeStatus == STATUS_SUCCESS
                || executeStatus == STATUS_FAILED
                || executeStatus == STATUS_CANCELLED);
}
```

### 3. ArchiveExecutor.checkCancellation()

Replace TODO with actual implementation:

- Call `repository.getTaskExecution(taskId)` to query current status
- If `executeStatus == STATUS_CANCELLING`, throw `TaskCancelledException`
- Called at: rule loop start (existing call site)

### 4. ArchiveGroupExecutor.run() Exception Handling

Catch `TaskCancelledException` separately from general exceptions:

- Publish `TaskEndEvent(success=false, cancelled=true, errorMsg="Task cancelled")`
- Update task status to CANCELLED(5) via repository

### 5. ArchiveLogRepository (interface)

New method:

```java
void updateTaskStatus(Long taskId, int status);
```

Implementation in `JdbcArchiveLogRepository`, delegating to Mapper.

### 6. ArchiveGroupExecuteTaskMapper

New method:

```java
void updateExecuteStatus(@Param("id") Long id, @Param("status") Integer status);
```

With corresponding XML mapping.

### 7. TaskEndEvent

Add field:

```java
private final boolean cancelled;
```

### 8. DbArchiveLogListener.handleTaskEnd()

Adapt for cancellation:

- If `event.isCancelled()`: set task status to CANCELLED(5), log type CANCEL
- Otherwise: keep existing logic (SUCCESS/FAILED, FINISH/ERROR)

### 9. ArchiveTaskController (new)

Path: `/api/v1/task-log/tasks`

| Method | Path | Description |
|--------|------|-------------|
| POST | `/{taskId}/cancel` | Cancel a running task |

Request body (optional): `{ "cancelReason": "..." }`

Logic:
1. Query task current status
2. Validate status is RUNNING(1), otherwise return error
3. Update status to CANCELLING(4)
4. Write CANCEL type TaskLog
5. Return success response

### 10. Error Handling

| Scenario | Handling |
|----------|----------|
| DB query failure during check | Log warning, retry next cycle, do not interrupt task |
| Cancel request on finished task | Return business error: "Task already ended" |
| Cancel request on CANCELLING task | Return success (idempotent) |
| Cancel request on WAITING task | Mark as CANCELLED directly |

## What We Are NOT Building (YAGNI)

- No separate cancel check interval config
- No TaskCancelledEvent (reuse TaskEndEvent with cancelled flag)
- No batch cancel
- No auto-retry after cancel
- No rollback of migrated data

## Files to Modify/Create

| File | Action | Module |
|------|--------|--------|
| `TaskCancelledException.java` | Create | easyarchive-core |
| `ArchiveGroupExecuteTask.java` | Modify | easyarchive-core |
| `ArchiveExecutor.java` | Modify | easyarchive-core |
| `ArchiveGroupExecutor.java` | Modify | easyarchive-core |
| `ArchiveLogRepository.java` | Modify | easyarchive-core |
| `TaskEndEvent.java` | Modify | easyarchive-core |
| `JdbcArchiveLogRepository.java` | Modify | easyarchive-starter |
| `ArchiveGroupExecuteTaskMapper.java` | Modify | easyarchive-starter |
| `ArchiveGroupExecuteTaskMapper.xml` | Modify | easyarchive-starter |
| `DbArchiveLogListener.java` | Modify | easyarchive-starter |
| `ArchiveTaskController.java` | Create | easyarchive-starter |
