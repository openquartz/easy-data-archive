# Archive Group Trigger And Cancel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete manual archive-group trigger and task-cancel management flow across backend APIs and frontend task/group pages.

**Architecture:** Extend starter-layer services with an aggregated archive-group runtime DTO and a group-scoped cancel command, while keeping the core execution engine's cooperative cancellation model unchanged. Update the Vue UI to consume the aggregated group runtime state and expose cancel actions consistently from group, task list, and task detail pages.

**Tech Stack:** Java 11, Spring Boot 2.3.2, MyBatis, JUnit 5, Mockito, Vue 3, TypeScript, Vite

---

### Task 1: Lock Down Backend Runtime-View Behavior With Tests

**Files:**
- Create: `docs/superpowers/specs/2026-06-02-archive-group-trigger-cancel-design.md`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`

- [ ] **Step 1: Write failing service and controller tests for aggregated group runtime fields and group-scoped cancel**

```java
@Test
void shouldExposeActiveTaskStateInGroupView() {
    ArchiveGroup group = enabledGroup();
    ArchiveGroupExecuteTask activeTask = activeTask(88L, ArchiveGroupExecuteTask.STATUS_RUNNING);
    when(groupMapper.selectList(null)).thenReturn(Arrays.asList(group));
    when(taskMapper.selectLatestActiveByGroupId(10L)).thenReturn(activeTask);

    List<ArchiveGroupView> result = service.findAll(null);

    assertEquals(1, result.size());
    assertEquals(88L, result.get(0).getActiveTaskId());
    assertEquals(ArchiveGroupExecuteTask.STATUS_RUNNING, result.get(0).getActiveTaskStatus());
    assertEquals(Boolean.FALSE, result.get(0).getCanTrigger());
    assertEquals(Boolean.TRUE, result.get(0).getCanCancelActiveTask());
}
```

```java
@Test
void shouldCancelLatestActiveTaskForGroup() {
    ArchiveGroup group = enabledGroup();
    ArchiveGroupExecuteTask activeTask = activeTask(88L, ArchiveGroupExecuteTask.STATUS_RUNNING);
    when(groupMapper.selectById(10L)).thenReturn(group);
    when(taskMapper.selectLatestActiveByGroupId(10L)).thenReturn(activeTask);

    ArchiveGroupExecuteTask cancelled = service.cancelActiveTask(10L, "user request");

    assertSame(activeTask, cancelled);
    verify(taskLogService).cancelTask(88L, "user request");
}
```

```java
@Test
void shouldExposeCancelActiveTaskEndpoint() throws Exception {
    ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
    task.setId(88L);
    task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLING);
    when(executionService.cancelActiveTask(eq(10L), any())).thenReturn(task);

    mockMvc.perform(post("/api/v1/archive/groups/10/cancel-active-task")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"cancelReason\":\"manual stop\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.id").value(88));
}
```

- [ ] **Step 2: Run backend tests to verify they fail for the missing behavior**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest,ArchiveGroupExecutionServiceImplTest,ArchiveGroupControllerContractTest test`
Expected: FAIL because `ArchiveGroupView`, `selectLatestActiveByGroupId`, and `cancelActiveTask` behavior do not exist yet.

- [ ] **Step 3: Commit after red state is observed**

```bash
git add docs/superpowers/specs/2026-06-02-archive-group-trigger-cancel-design.md easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java
git commit -m "test: define archive group runtime view behavior"
```

### Task 2: Implement Backend Aggregation, Group Cancel API, And Restrictions

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupExecutionService.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`

- [ ] **Step 1: Create the group runtime DTO**

```java
public class ArchiveGroupView extends ArchiveGroup {
    private Long activeTaskId;
    private Integer activeTaskStatus;
    private Date activeTaskStartTime;
    private Boolean canTrigger;
    private Boolean canCancelActiveTask;
    private Boolean canViewActiveTask;
}
```

- [ ] **Step 2: Add mapper query for latest active task**

```xml
<select id="selectLatestActiveByGroupId" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM ea_archive_group_execute_task
    WHERE deleted = 0
      AND group_id = #{groupId}
      AND execute_status IN (0, 1, 4)
    ORDER BY id DESC
    LIMIT 1
</select>
```

- [ ] **Step 3: Implement group runtime aggregation and operation restrictions**

```java
private ArchiveGroupView toView(ArchiveGroup group) {
    ArchiveGroupExecuteTask activeTask = taskMapper.selectLatestActiveByGroupId(group.getId());
    ArchiveGroupView view = new ArchiveGroupView();
    BeanUtils.copyProperties(group, view);
    if (activeTask != null) {
        view.setActiveTaskId(activeTask.getId());
        view.setActiveTaskStatus(activeTask.getExecuteStatus());
        view.setActiveTaskStartTime(activeTask.getStartTime());
    }
    boolean hasActiveTask = activeTask != null;
    view.setCanTrigger(!hasActiveTask && Integer.valueOf(0).equals(group.getEnableStatus()));
    view.setCanCancelActiveTask(hasActiveTask);
    view.setCanViewActiveTask(hasActiveTask);
    return view;
}
```

- [ ] **Step 4: Implement group-scoped cancel command**

```java
@Override
public ArchiveGroupExecuteTask cancelActiveTask(Long groupId, String cancelReason) {
    requireEnabledGroupOrExistingGroup(groupId);
    ArchiveGroupExecuteTask activeTask = taskMapper.selectLatestActiveByGroupId(groupId);
    if (activeTask == null) {
        throw new IllegalStateException("Archive group has no active task");
    }
    taskLogService.cancelTask(activeTask.getId(), cancelReason);
    return activeTask;
}
```

- [ ] **Step 5: Expose the new controller endpoint and switch group reads to the view DTO**

```java
@PostMapping("/{id}/cancel-active-task")
public ApiResponse<ArchiveGroupExecuteTask> cancelActiveTask(
        @PathVariable Long id,
        @RequestBody(required = false) Map<String, String> body) {
    String reason = body != null ? body.get("cancelReason") : null;
    return ApiResponse.success(executionService.cancelActiveTask(id, reason));
}
```

- [ ] **Step 6: Run targeted backend tests to verify green**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest,ArchiveGroupExecutionServiceImplTest,ArchiveGroupControllerContractTest test`
Expected: PASS with the new DTO, mapper query, and cancel-active-task endpoint in place.

- [ ] **Step 7: Commit backend runtime-view implementation**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupExecutionService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml
git commit -m "feat: add archive group runtime view and group cancel api"
```

### Task 3: Add Frontend Contract Coverage For Trigger/Cancel Actions

**Files:**
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/api/task.ts`
- Modify: `easyarchive-ui/scripts/smoke-check.mjs`

- [ ] **Step 1: Add failing frontend contract checks for new fields and endpoints**

```javascript
assertType(group.activeTaskId, ['number', 'undefined']);
assertType(group.activeTaskStatus, ['number', 'undefined']);
assertType(group.canTrigger, ['boolean', 'undefined']);
assertType(group.canCancelActiveTask, ['boolean', 'undefined']);
```

```typescript
export function cancelArchiveGroupActiveTaskApi(groupId: number, cancelReason?: string): Promise<TaskItem> {
  return http.post<TaskItem>(`/archive/groups/${groupId}/cancel-active-task`, { cancelReason });
}
```

- [ ] **Step 2: Run frontend contract verification to confirm red state**

Run: `npm --prefix easyarchive-ui run smoke`
Expected: FAIL until the API contracts and UI behavior are updated.

- [ ] **Step 3: Commit red-state contract changes**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts easyarchive-ui/src/api/task.ts easyarchive-ui/scripts/smoke-check.mjs
git commit -m "test: define archive group runtime api contract"
```

### Task 4: Implement Frontend Group/Task Action Flow

**Files:**
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/api/task.ts`
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Modify: `easyarchive-ui/src/views/TaskListView.vue`
- Modify: `easyarchive-ui/src/views/TaskDetailView.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: Extend the group and task API models**

```typescript
export interface ArchiveGroup {
  id: number;
  groupCode: string;
  groupName: string;
  enableStatus: number;
  activeTaskId?: number;
  activeTaskStatus?: number;
  activeTaskStartTime?: string;
  canTrigger?: boolean;
  canCancelActiveTask?: boolean;
  canViewActiveTask?: boolean;
}
```

- [ ] **Step 2: Update the archive group page action logic**

```typescript
const hasActiveTasks = computed(() => groups.value.some((item) => typeof item.activeTaskId === "number"));

async function cancelGroupTask(group: ArchiveGroup): Promise<void> {
  await runGroupAction("cancelTask", group.id, async () => {
    const task = await cancelArchiveGroupActiveTaskApi(group.id);
    successMessage.value = t("archiveGroup.cancelSubmitted").replace("{id}", String(task.id));
    await loadData();
  });
}
```

- [ ] **Step 3: Add task-list cancel action**

```vue
<button
  v-if="item.executeStatus === 0 || item.executeStatus === 1"
  class="btn btn--subtle"
  @click="cancelTask(item)"
>
  {{ t("task.cancelAction") }}
</button>
```

- [ ] **Step 4: Align task-detail cancelling UI and i18n**

```typescript
const cancelButtonText = computed(() =>
  task.value?.executeStatus === 4 ? t("task.cancelling") : t("task.cancelAction")
);
```

- [ ] **Step 5: Run frontend smoke/build verification**

Run: `npm --prefix easyarchive-ui run build`
Expected: PASS with updated archive group and task action flows.

Run: `npm --prefix easyarchive-ui run smoke`
Expected: PASS with updated contracts.

- [ ] **Step 6: Commit frontend runtime-flow implementation**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts easyarchive-ui/src/api/task.ts easyarchive-ui/src/views/ArchiveGroupView.vue easyarchive-ui/src/views/TaskListView.vue easyarchive-ui/src/views/TaskDetailView.vue easyarchive-ui/src/i18n/messages.ts easyarchive-ui/scripts/smoke-check.mjs
git commit -m "feat: wire archive group trigger and cancel actions in ui"
```

### Task 5: Final Verification

**Files:**
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImplTest.java`
- Modify: any files touched above as needed

- [ ] **Step 1: Add or update cancellation-state regression tests if needed**

```java
@Test
void shouldKeepCancellingTaskIdempotent() {
    ArchiveGroupExecuteTask task = runningTask(88L);
    task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_CANCELLING);
    when(archiveLogRepository.queryTaskById(88L)).thenReturn(task);

    service.cancelTask(88L, "again");

    verify(archiveLogRepository, never()).updateTaskStatus(anyLong(), anyInt());
}
```

- [ ] **Step 2: Run full targeted verification**

Run: `mvn -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest,ArchiveGroupExecutionServiceImplTest,ArchiveTaskLogServiceImplTest,ArchiveGroupControllerContractTest test`
Expected: PASS

Run: `npm --prefix easyarchive-ui run build`
Expected: PASS

Run: `npm --prefix easyarchive-ui run smoke`
Expected: PASS

- [ ] **Step 3: Re-read the spec and check requirement coverage**

```text
Verify manual group trigger, active-task visibility, group cancel, task-list cancel, task-detail cancel, and cooperative cancellation semantics are all present in code and tests.
```
