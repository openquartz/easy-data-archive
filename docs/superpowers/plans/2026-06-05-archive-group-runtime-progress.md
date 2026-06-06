# Archive Group Runtime Progress Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add archive-group runtime progress display by exposing active-task progress snapshots from the backend and rendering a simulated progress bar plus migrated-record count in the archive-group list and detail views.

**Architecture:** Extend the existing `ArchiveGroupView` DTO with active-task snapshot fields sourced from `ArchiveGroupExecuteTask`, then reuse the existing group list and overview APIs without adding new queries. On the frontend, centralize runtime-progress behavior in `archiveGroupRuntime.ts`, extend API types, and render the simulated progress UI in the archive-group list and detail views using the same helper functions and i18n labels.

**Tech Stack:** Java 11, Spring Boot 2.3.2, MyBatis, JUnit 5, Vue 3, TypeScript, node:test

---

### Task 1: Backend DTO contract for active-task runtime snapshots

**Files:**
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`

- [x] **Step 1: Write the failing backend service test for snapshot mapping**

Add a focused test to `ArchiveGroupServiceImplTest` that proves an active task's `processedRecords`, `processedSpeed`, and `heartbeatTime` are copied into `ArchiveGroupView`.

```java
@Test
void shouldExposeActiveTaskRuntimeSnapshotInGroupView() {
    ArchiveGroup group = enabledGroup();
    group.setId(10L);
    when(groupMapper.selectList(null)).thenReturn(Arrays.asList(group));

    ArchiveGroupExecuteTask activeTask = new ArchiveGroupExecuteTask();
    activeTask.setId(88L);
    activeTask.setGroupId(10L);
    activeTask.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
    activeTask.setProcessedRecords(1234L);
    activeTask.setProcessedSpeed(new BigDecimal("56.78"));
    Date heartbeatTime = new Date();
    activeTask.setHeartbeatTime(heartbeatTime);
    when(taskMapper.selectLatestActiveByGroupIds(anyList())).thenReturn(Arrays.asList(activeTask));

    List<ArchiveGroupView> result = service.findAll(null);

    assertEquals(1, result.size());
    assertEquals(1234L, result.get(0).getActiveTaskProcessedRecords());
    assertEquals(new BigDecimal("56.78"), result.get(0).getActiveTaskProcessedSpeed());
    assertEquals(heartbeatTime, result.get(0).getActiveTaskHeartbeatTime());
}
```

- [x] **Step 2: Run the backend service test to verify it fails**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest#shouldExposeActiveTaskRuntimeSnapshotInGroupView test`

Expected: FAIL with missing getter/setter or assertion failure because `ArchiveGroupView` does not yet expose the new snapshot fields.

- [x] **Step 3: Write the failing controller contract assertions**

Extend `ArchiveGroupControllerContractTest` list and overview cases to expect the new fields in the JSON payload.

```java
group.setActiveTaskProcessedRecords(1234L);
group.setActiveTaskProcessedSpeed(new BigDecimal("56.78"));
group.setActiveTaskHeartbeatTime(new Date(1704067200000L));

.andExpect(jsonPath("$.data[0].activeTaskProcessedRecords").value(1234))
.andExpect(jsonPath("$.data[0].activeTaskProcessedSpeed").value(56.78))
.andExpect(jsonPath("$.data[0].activeTaskHeartbeatTime").exists())
```

For the overview response:

```java
group.setActiveTaskProcessedRecords(4321L);
group.setActiveTaskProcessedSpeed(new BigDecimal("12.34"));
group.setActiveTaskHeartbeatTime(new Date(1704067200000L));

.andExpect(jsonPath("$.data.group.activeTaskProcessedRecords").value(4321))
.andExpect(jsonPath("$.data.group.activeTaskProcessedSpeed").value(12.34))
.andExpect(jsonPath("$.data.group.activeTaskHeartbeatTime").exists())
```

- [x] **Step 4: Run the controller contract test to verify it fails**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupControllerContractTest test`

Expected: FAIL because the serialized JSON does not contain the new fields yet.

- [x] **Step 5: Write the minimal backend implementation**

Extend `ArchiveGroupView` with the three new fields.

```java
private Long activeTaskProcessedRecords;
private BigDecimal activeTaskProcessedSpeed;
private Date activeTaskHeartbeatTime;
```

Update `ArchiveGroupServiceImpl.toView(group, activeTask)` to copy them when an active task exists.

```java
if (hasActiveTask) {
    view.setActiveTaskId(activeTask.getId());
    view.setActiveTaskStatus(activeTask.getExecuteStatus());
    view.setActiveTaskStartTime(activeTask.getStartTime());
    view.setActiveTaskProcessedRecords(activeTask.getProcessedRecords());
    view.setActiveTaskProcessedSpeed(activeTask.getProcessedSpeed());
    view.setActiveTaskHeartbeatTime(activeTask.getHeartbeatTime());
}
```

- [x] **Step 6: Run the backend tests to verify they pass**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest#shouldExposeActiveTaskRuntimeSnapshotInGroupView,ArchiveGroupControllerContractTest test`

Expected: PASS with `BUILD SUCCESS`.

- [x] **Step 7: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java
git commit -m "feat: expose archive group active task snapshots"
```

### Task 2: Frontend runtime helpers and API typing

**Files:**
- Modify: `easyarchive-ui/tests/archive-group-runtime.test.ts`
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/utils/archiveGroupRuntime.ts`

- [x] **Step 1: Write the failing runtime-helper tests**

Extend `archive-group-runtime.test.ts` with coverage for migrated-record extraction and simulated progress calculation.

```ts
import {
  getArchiveGroupRuntimeProcessedRecords,
  resolveArchiveGroupRuntimeProgress
} from "../src/utils/archiveGroupRuntime";

test("running archive group with processed records exposes migrated count and simulated progress", () => {
  const group = {
    id: 3,
    activeTaskId: 99,
    activeTaskStatus: 1,
    activeTaskProcessedRecords: 1000
  };

  assert.equal(getArchiveGroupRuntimeProcessedRecords(group), 1000);
  assert.equal(resolveArchiveGroupRuntimeProgress(group), 67);
});

test("cancelling archive group is capped at 95 percent", () => {
  const group = {
    id: 4,
    activeTaskId: 100,
    activeTaskStatus: 4,
    activeTaskProcessedRecords: 200
  };

  assert.equal(resolveArchiveGroupRuntimeProgress(group), 95);
});

test("successful archive group reports full progress", () => {
  const group = {
    id: 5,
    activeTaskId: 101,
    activeTaskStatus: 2,
    activeTaskProcessedRecords: 1
  };

  assert.equal(resolveArchiveGroupRuntimeProgress(group), 100);
});
```

- [x] **Step 2: Run the frontend runtime-helper test to verify it fails**

Run: `cd easyarchive-ui && node --test tests/archive-group-runtime.test.ts`

Expected: FAIL with missing exports from `archiveGroupRuntime.ts`.

- [x] **Step 3: Extend the archive-group API type**

Add the new backend snapshot fields to `ArchiveGroup` and exclude nothing else from `ArchiveGroupPayload`.

```ts
activeTaskProcessedRecords?: number;
activeTaskProcessedSpeed?: number;
activeTaskHeartbeatTime?: string;
```

- [x] **Step 4: Write the minimal runtime helper implementation**

Extend `ArchiveGroupRuntimeState` and add two helpers in `archiveGroupRuntime.ts`.

```ts
export interface ArchiveGroupRuntimeState {
  activeTaskId?: number;
  activeTaskStatus?: number;
  activeTaskProcessedRecords?: number;
  canTrigger?: boolean;
  canCancelActiveTask?: boolean;
  canViewActiveTask?: boolean;
}

export function getArchiveGroupRuntimeProcessedRecords(group?: ArchiveGroupRuntimeState | null): number {
  return typeof group?.activeTaskProcessedRecords === "number" && group.activeTaskProcessedRecords > 0
    ? group.activeTaskProcessedRecords
    : 0;
}

export function resolveArchiveGroupRuntimeProgress(group?: ArchiveGroupRuntimeState | null): number {
  if (!hasArchiveGroupActiveTask(group)) {
    return 0;
  }
  if (group?.activeTaskStatus === 2) {
    return 100;
  }
  if (group?.activeTaskStatus === 4) {
    return 95;
  }

  const processedRecords = getArchiveGroupRuntimeProcessedRecords(group);
  if (processedRecords <= 0) {
    return 0;
  }

  if (group?.activeTaskStatus === 1) {
    return Math.min(95, 12 + Math.min(83, Math.floor(Math.log(processedRecords + 1) * 8)));
  }

  if (group?.activeTaskStatus === 3 || group?.activeTaskStatus === 5) {
    return Math.min(95, 12 + Math.min(83, Math.floor(Math.log(processedRecords + 1) * 8)));
  }

  return 0;
}
```

- [x] **Step 5: Run the frontend runtime-helper test to verify it passes**

Run: `cd easyarchive-ui && node --test tests/archive-group-runtime.test.ts`

Expected: PASS with all archive-group runtime tests green.

- [x] **Step 6: Commit**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts \
  easyarchive-ui/src/utils/archiveGroupRuntime.ts \
  easyarchive-ui/tests/archive-group-runtime.test.ts
git commit -m "feat: add archive group runtime progress helpers"
```

### Task 3: Render runtime progress in archive-group list and detail views

**Files:**
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Modify: `easyarchive-ui/src/views/ArchiveGroupDetailView.vue`

- [x] **Step 1: Write the failing UI-facing assertions by expanding runtime tests**

Add snapshot-like behavioral tests for the helper-driven labels that the views will depend on.

```ts
test("running archive group progress grows monotonically with processed records", () => {
  const low = resolveArchiveGroupRuntimeProgress({
    id: 6,
    activeTaskId: 200,
    activeTaskStatus: 1,
    activeTaskProcessedRecords: 10
  });
  const high = resolveArchiveGroupRuntimeProgress({
    id: 6,
    activeTaskId: 200,
    activeTaskStatus: 1,
    activeTaskProcessedRecords: 10000
  });

  assert.ok(low > 0);
  assert.ok(high >= low);
  assert.ok(high < 100);
});
```

- [x] **Step 2: Run the frontend runtime test suite to verify the new assertion fails when helper behavior is incomplete**

Run: `cd easyarchive-ui && node --test tests/archive-group-runtime.test.ts`

Expected: FAIL if the helper logic is not yet monotonic or does not cap running progress below `100`.

- [x] **Step 3: Add the i18n labels required by the new UI**

Extend `messages.ts` for both locales with:

```ts
archiveGroup: {
  columns: {
    runtimeProgress: "运行进度",
    migratedRecords: "已迁移成功总数"
  }
}
```

and for English:

```ts
archiveGroup: {
  columns: {
    runtimeProgress: "Runtime Progress",
    migratedRecords: "Migrated Records"
  }
}
```

- [x] **Step 4: Render the list-view runtime progress UI**

Update `ArchiveGroupView.vue` imports and template to use the runtime helpers and add a new column.

```ts
import {
  canCancelArchiveGroupActiveTask,
  canTriggerArchiveGroup,
  canViewArchiveGroupActiveTask,
  getArchiveGroupRuntimeProcessedRecords,
  hasArchiveGroupActiveTask,
  resolveArchiveGroupRuntimeProgress
} from "../utils/archiveGroupRuntime";
```

In the table header:

```vue
<th>{{ t("archiveGroup.columns.runtimeProgress") }}</th>
```

In each row:

```vue
<td>
  <div v-if="hasArchiveGroupActiveTask(group)" class="runtime-progress">
    <div class="runtime-progress__bar">
      <span class="runtime-progress__fill" :style="{ width: `${resolveArchiveGroupRuntimeProgress(group)}%` }" />
    </div>
    <div class="runtime-progress__meta">
      <span>{{ resolveArchiveGroupRuntimeProgress(group) }}%</span>
      <span>{{ t("archiveGroup.columns.migratedRecords") }}: {{ getArchiveGroupRuntimeProcessedRecords(group) }}</span>
    </div>
  </div>
  <span v-else>0</span>
</td>
```

Add the minimal scoped CSS classes required to make the bar visible and compact.

- [x] **Step 5: Render the detail-view runtime progress UI**

Update `ArchiveGroupDetailView.vue` imports and current-task card content to show the same progress bar and migrated-record count.

```ts
import {
  canTriggerArchiveGroup,
  canViewArchiveGroupActiveTask,
  getArchiveGroupRuntimeProcessedRecords,
  hasArchiveGroupActiveTask,
  resolveArchiveGroupRuntimeProgress
} from "../utils/archiveGroupRuntime";
```

In the current-task section:

```vue
<div v-if="group && hasArchiveGroupActiveTask(group)" class="runtime-progress runtime-progress--detail">
  <p><strong>{{ t("archiveGroup.columns.migratedRecords") }}:</strong> {{ getArchiveGroupRuntimeProcessedRecords(group) }}</p>
  <div class="runtime-progress__bar">
    <span class="runtime-progress__fill" :style="{ width: `${resolveArchiveGroupRuntimeProgress(group)}%` }" />
  </div>
  <p><strong>{{ t("archiveGroup.columns.runtimeProgress") }}:</strong> {{ resolveArchiveGroupRuntimeProgress(group) }}%</p>
</div>
```

- [x] **Step 6: Run the frontend runtime test suite to verify it passes**

Run: `cd easyarchive-ui && node --test tests/archive-group-runtime.test.ts`

Expected: PASS with all helper-driven runtime behavior green.

- [x] **Step 7: Run a production frontend check**

Run: `cd easyarchive-ui && npm run build`

Expected: PASS with a successful production build.

- [x] **Step 8: Commit**

```bash
git add easyarchive-ui/src/i18n/messages.ts \
  easyarchive-ui/src/views/ArchiveGroupView.vue \
  easyarchive-ui/src/views/ArchiveGroupDetailView.vue
git commit -m "feat: show archive group runtime progress"
```

### Task 4: Final verification

**Files:**
- Modify: `docs/superpowers/plans/2026-06-05-archive-group-runtime-progress.md`

- [x] **Step 1: Mark completed steps in this plan**

Update this plan file by checking off each completed checkbox in order so execution state is accurate.

- [x] **Step 2: Run the targeted backend verification**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest,ArchiveGroupControllerContractTest test`

Expected: PASS with `BUILD SUCCESS`.

- [x] **Step 3: Run the targeted frontend verification**

Run: `cd easyarchive-ui && node --test tests/archive-group-runtime.test.ts && npm run build`

Expected: PASS with all tests green and a successful build.

- [x] **Step 4: Review changed files against the spec**

Confirm the implementation still matches:

```text
1. Backend only exposes active-task raw snapshot fields.
2. Frontend computes simulated progress.
3. No database migration was added.
4. No historical cumulative total was introduced.
```

- [x] **Step 5: Commit any plan checkbox updates if needed**

```bash
git add docs/superpowers/plans/2026-06-05-archive-group-runtime-progress.md
git commit -m "docs: update archive group runtime progress plan status"
```
