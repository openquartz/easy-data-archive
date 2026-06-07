# Archive Executor Range Log Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist the resolved source table and effective execution range into task logs before `ArchiveExecutor` processes time-based or id-based rules.

**Architecture:** Keep the existing event flow unchanged. Add a focused helper inside `ArchiveExecutor` that writes one extra `ArchiveTaskLog` record through `ArchiveLogRepository` for time-range and id-range rules, then cover the new behavior with a core unit test.

**Tech Stack:** Java 11, JUnit 5, Mockito, Maven

---

### Task 1: Implement range log persistence in `ArchiveExecutor`

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/executor/ArchiveExecutor.java`

- [ ] **Step 1: Add a failing test target by identifying the new helper contract**

Expected helper responsibilities:

```java
void logTimeRuleRange(String sourceTable, Date startDate, Date endDate)
void logIdRuleRange(String sourceTable, Long startId, Long endId)
```

- [ ] **Step 2: Implement the minimal helper methods and wire them into `run`**

Required content:

```java
logTimeRuleRange(this.currentSourceTable, startDate, endDate);
logIdRuleRange(this.currentSourceTable, startId, endId);
```

The helper must create `ArchiveTaskLog` with:

```java
log.setTaskId(taskId);
log.setLogType(ArchiveTaskLogTypeEnum.START);
log.setLogLevel(ArchiveTaskLogLevelEnum.INFO);
log.setExecutePhase(ArchiveTaskExecutePhaseEnum.RULE_START);
```

- [ ] **Step 3: Keep formatting logic local and deterministic**

Use a local formatter:

```java
DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
```

The log content should contain:

```text
规则范围:<actualTable>, 时间范围:<start> -> <end>
规则范围:<actualTable>, ID范围:<startId> -> <endId>
```

### Task 2: Add focused unit coverage for the new helper behavior

**Files:**
- Create: `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/executor/ArchiveExecutorTest.java`
- Test: `easyarchive-core/src/test/java/com/openquartz/easyarchive/core/executor/ArchiveExecutorTest.java`

- [ ] **Step 1: Write the failing tests**

```java
@Test
void shouldSaveTimeRuleRangeLog() { }

@Test
void shouldSaveIdRuleRangeLog() { }
```

- [ ] **Step 2: Run the focused test target to verify it fails**

Run: `mvn -pl easyarchive-core -Dtest=ArchiveExecutorTest test`

Expected: FAIL because the helper methods and assertions do not exist yet.

- [ ] **Step 3: Implement the test assertions against saved log content**

Verify:

```java
verify(archiveLogRepository).saveTaskLog(captor.capture());
assertEquals("RULE_START", log.getExecutePhase());
assertTrue(log.getLogContent().contains("时间范围:"));
assertTrue(log.getLogContent().contains("ID范围:"));
```

- [ ] **Step 4: Run the focused test target to verify it passes**

Run: `mvn -pl easyarchive-core -Dtest=ArchiveExecutorTest test`

Expected: PASS
