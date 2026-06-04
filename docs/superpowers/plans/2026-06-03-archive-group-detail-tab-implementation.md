# Archive Group Detail Tab Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build an internal tab-based archive group detail page that shows group metadata, merged group items, task overview stats, and recent task history.

**Architecture:** Keep the existing archive group list page as the entry point and add a dedicated detail route under the current authenticated `AppLayout`. On the backend, add a focused `/overview` aggregation endpoint instead of bloating the existing group detail API. On the frontend, add a lightweight tab host in the main layout, a detail view that loads three data blocks in parallel, and API types aligned to the new backend DTOs.

**Tech Stack:** Vue 3 + Vue Router + TypeScript, Spring Boot 2.3.2, MyBatis XML mappers, JUnit 5 + Mockito, MockMvc

---

## File Structure

### Backend

- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
  Add `GET /api/v1/archive/groups/{id}/overview`.
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`
  Expose a detail-page aggregation method.
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
  Implement aggregation by composing group data, item counts, task stats, and recent tasks.
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
  Add group-scoped stats and recent-task query methods.
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`
  Implement SQL for group task counts and recent tasks.
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java`
  Add group-scoped count methods if not already present.
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByIdMapper.xml`
  Implement group-scoped count methods.
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java`
  Add group-scoped count methods if not already present.
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByTimeMapper.xml`
  Implement group-scoped count methods.
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupOverviewView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemStatsView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupTaskStatsView.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`

### Frontend

- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
  Add overview types and fetcher.
- Modify: `easyarchive-ui/src/router/index.ts`
  Add archive group detail route.
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
  Add internal tab host and dedupe/open-close behavior.
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
  Wire the existing “明细” action to open the internal detail tab instead of relying on the list/detail split inside one page.
- Create: `easyarchive-ui/src/views/ArchiveGroupDetailView.vue`
  Render page header, group detail card, merged item table, task summary, and recent tasks.
- Modify: `easyarchive-ui/src/i18n/messages.ts`
  Add strings for the new detail page and tab labels.
- Modify: `easyarchive-ui/src/styles/theme.css`
  Add layout and card/table styles used by the new detail page and tab host.
- Test/verify: `easyarchive-ui/scripts/smoke-check.mjs`
  Extend smoke assertions if it currently checks route-level rendering only.

## Task 1: Add Backend Overview Contract

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupOverviewView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemStatsView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupTaskStatsView.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`

- [ ] **Step 1: Write the failing controller contract test**

```java
@Test
void shouldReturnArchiveGroupOverviewEnvelope() throws Exception {
    ArchiveGroupView group = new ArchiveGroupView();
    group.setId(10L);
    group.setGroupCode("ORDER_ARCHIVE");
    group.setGroupName("Order Archive");

    ArchiveGroupItemStatsView itemStats = new ArchiveGroupItemStatsView();
    itemStats.setTotalCount(12);
    itemStats.setEnabledCount(10);
    itemStats.setDisabledCount(2);
    itemStats.setIdTypeCount(7);
    itemStats.setTimeTypeCount(5);

    ArchiveGroupTaskStatsView taskStats = new ArchiveGroupTaskStatsView();
    taskStats.setTotalCount(28);
    taskStats.setSuccessCount(24);
    taskStats.setFailedCount(3);
    taskStats.setRunningCount(1);

    ArchiveGroupExecuteTask task = new ArchiveGroupExecuteTask();
    task.setId(101L);
    task.setGroupId(10L);
    task.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_RUNNING);
    task.setProcessedRecords(12000L);

    ArchiveGroupOverviewView overview = new ArchiveGroupOverviewView();
    overview.setGroup(group);
    overview.setItemStats(itemStats);
    overview.setTaskStats(taskStats);
    overview.setRecentTasks(Collections.singletonList(task));

    when(groupService.findOverview(10L)).thenReturn(overview);

    mockMvc.perform(get("/api/v1/archive/groups/10/overview"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("SUCCESS"))
            .andExpect(jsonPath("$.data.group.id").value(10))
            .andExpect(jsonPath("$.data.itemStats.totalCount").value(12))
            .andExpect(jsonPath("$.data.taskStats.runningCount").value(1))
            .andExpect(jsonPath("$.data.recentTasks[0].id").value(101));
}
```

- [ ] **Step 2: Run the controller contract test to verify it fails**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupControllerContractTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `findOverview` and `/overview` endpoint do not exist yet.

- [ ] **Step 3: Add DTOs and service/controller contract**

```java
// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupOverviewView.java
@Data
public class ArchiveGroupOverviewView {
    private ArchiveGroupView group;
    private ArchiveGroupItemStatsView itemStats;
    private ArchiveGroupTaskStatsView taskStats;
    private List<ArchiveGroupExecuteTask> recentTasks;
}

// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemStatsView.java
@Data
public class ArchiveGroupItemStatsView {
    private Integer totalCount;
    private Integer enabledCount;
    private Integer disabledCount;
    private Integer idTypeCount;
    private Integer timeTypeCount;
}

// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupTaskStatsView.java
@Data
public class ArchiveGroupTaskStatsView {
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private Integer runningCount;
    private Integer lastExecuteStatus;
    private Date lastExecuteTime;
}
```

```java
// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java
ArchiveGroupOverviewView findOverview(Long id);
```

```java
// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java
@GetMapping("/{id}/overview")
public ApiResponse<ArchiveGroupOverviewView> overview(@PathVariable Long id) {
    return ApiResponse.success(groupService.findOverview(id));
}
```

- [ ] **Step 4: Re-run the controller contract test**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupControllerContractTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS for the new `/overview` contract.

- [ ] **Step 5: Commit the backend contract slice**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupOverviewView.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemStatsView.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupTaskStatsView.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java
git commit -m "feat: add archive group overview contract"
```

## Task 2: Implement Backend Overview Aggregation

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByIdMapper.xml`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByTimeMapper.xml`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`

- [ ] **Step 1: Write the failing service tests for item/task aggregation**

```java
@Test
void shouldBuildOverviewWithItemAndTaskStats() {
    ArchiveGroup group = enabledGroup();
    when(groupMapper.selectById(10L)).thenReturn(group);

    ArchiveGroupExecuteTask latest = new ArchiveGroupExecuteTask();
    latest.setId(200L);
    latest.setExecuteStatus(ArchiveGroupExecuteTask.STATUS_SUCCESS);
    latest.setStartTime(new Date());
    when(taskMapper.selectLatestByGroupId(10L)).thenReturn(latest);
    when(taskMapper.countByGroupId(10L)).thenReturn(9);
    when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_SUCCESS)).thenReturn(7);
    when(taskMapper.countByGroupIdAndStatus(10L, ArchiveGroupExecuteTask.STATUS_FAILED)).thenReturn(1);
    when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);
    when(taskMapper.selectRecentByGroupId(10L, 10)).thenReturn(Arrays.asList(latest));
    when(idItemMapper.countByGroupId(10L)).thenReturn(4);
    when(idItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(3);
    when(idItemMapper.countByGroupIdAndStatus(10L, 1)).thenReturn(1);
    when(timeItemMapper.countByGroupId(10L)).thenReturn(2);
    when(timeItemMapper.countByGroupIdAndStatus(10L, 0)).thenReturn(2);
    when(timeItemMapper.countByGroupIdAndStatus(10L, 1)).thenReturn(0);

    ArchiveGroupOverviewView overview = service.findOverview(10L);

    assertEquals(6, overview.getItemStats().getTotalCount());
    assertEquals(5, overview.getItemStats().getEnabledCount());
    assertEquals(1, overview.getItemStats().getDisabledCount());
    assertEquals(9, overview.getTaskStats().getTotalCount());
    assertEquals(7, overview.getTaskStats().getSuccessCount());
    assertEquals(1, overview.getTaskStats().getFailedCount());
    assertEquals(1, overview.getTaskStats().getRunningCount());
    assertEquals(ArchiveGroupExecuteTask.STATUS_SUCCESS, overview.getTaskStats().getLastExecuteStatus());
    assertEquals(1, overview.getRecentTasks().size());
}
```

```java
@Test
void shouldRejectOverviewLookupWhenGroupDoesNotExist() {
    when(groupMapper.selectById(10L)).thenReturn(null);
    assertThrows(IllegalArgumentException.class, () -> service.findOverview(10L));
}
```

- [ ] **Step 2: Run the service tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL because `findOverview`, mapper methods, and constructor dependencies are incomplete.

- [ ] **Step 3: Add mapper methods needed by the aggregation**

```java
// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java
int countByGroupId(@Param("groupId") Long groupId);
int countByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("status") Integer status);
ArchiveGroupExecuteTask selectLatestByGroupId(@Param("groupId") Long groupId);
List<ArchiveGroupExecuteTask> selectRecentByGroupId(@Param("groupId") Long groupId, @Param("limit") int limit);
```

```xml
<!-- easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml -->
<select id="countByGroupId" resultType="int">
    SELECT COUNT(1)
    FROM ea_archive_group_execute_task
    WHERE deleted = 0 AND group_id = #{groupId}
</select>

<select id="countByGroupIdAndStatus" resultType="int">
    SELECT COUNT(1)
    FROM ea_archive_group_execute_task
    WHERE deleted = 0
      AND group_id = #{groupId}
      AND execute_status = #{status}
</select>

<select id="selectLatestByGroupId" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM ea_archive_group_execute_task
    WHERE deleted = 0 AND group_id = #{groupId}
    ORDER BY id DESC
    LIMIT 1
</select>

<select id="selectRecentByGroupId" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM ea_archive_group_execute_task
    WHERE deleted = 0 AND group_id = #{groupId}
    ORDER BY id DESC
    LIMIT #{limit}
</select>
```

```java
// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java
int countByGroupId(@Param("groupId") Long groupId);
int countByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);
```

```java
// easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java
int countByGroupId(@Param("groupId") Long groupId);
int countByGroupIdAndStatus(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);
```

- [ ] **Step 4: Implement `findOverview` in the service**

```java
@Override
public ArchiveGroupOverviewView findOverview(Long id) {
    ArchiveGroup group = ensureExists(id);

    ArchiveGroupOverviewView overview = new ArchiveGroupOverviewView();
    overview.setGroup(toView(group));
    overview.setItemStats(buildItemStats(id));
    overview.setTaskStats(buildTaskStats(id));
    overview.setRecentTasks(taskMapper.selectRecentByGroupId(id, 10));
    return overview;
}

private ArchiveGroupItemStatsView buildItemStats(Long groupId) {
    int idTotal = idItemMapper.countByGroupId(groupId);
    int timeTotal = timeItemMapper.countByGroupId(groupId);
    int enabled = idItemMapper.countByGroupIdAndStatus(groupId, 0)
            + timeItemMapper.countByGroupIdAndStatus(groupId, 0);
    int disabled = idItemMapper.countByGroupIdAndStatus(groupId, 1)
            + timeItemMapper.countByGroupIdAndStatus(groupId, 1);

    ArchiveGroupItemStatsView stats = new ArchiveGroupItemStatsView();
    stats.setTotalCount(idTotal + timeTotal);
    stats.setEnabledCount(enabled);
    stats.setDisabledCount(disabled);
    stats.setIdTypeCount(idTotal);
    stats.setTimeTypeCount(timeTotal);
    return stats;
}

private ArchiveGroupTaskStatsView buildTaskStats(Long groupId) {
    ArchiveGroupExecuteTask latest = taskMapper.selectLatestByGroupId(groupId);

    ArchiveGroupTaskStatsView stats = new ArchiveGroupTaskStatsView();
    stats.setTotalCount(taskMapper.countByGroupId(groupId));
    stats.setSuccessCount(taskMapper.countByGroupIdAndStatus(groupId, ArchiveGroupExecuteTask.STATUS_SUCCESS));
    stats.setFailedCount(taskMapper.countByGroupIdAndStatus(groupId, ArchiveGroupExecuteTask.STATUS_FAILED));
    stats.setRunningCount(taskMapper.countActiveByGroupId(groupId));
    if (latest != null) {
        stats.setLastExecuteStatus(latest.getExecuteStatus());
        stats.setLastExecuteTime(latest.getStartTime());
    }
    return stats;
}
```

Notes:
- Update the service constructor/dependencies to include both item mappers.
- Keep `toView(group)` as the source of active-task booleans for the page header.

- [ ] **Step 5: Re-run the service tests**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS for the new overview aggregation behavior.

- [ ] **Step 6: Commit the backend implementation slice**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByIdMapper.xml easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByTimeMapper.xml easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java
git commit -m "feat: add archive group overview aggregation"
```

## Task 3: Add Frontend API Types and Detail Route

**Files:**
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/router/index.ts`
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: Write the failing route/API smoke expectation**

Add a route assertion to the existing smoke script or, if it is not route-aware, add a minimal exported route check:

```ts
const detailRoute = router.getRoutes().find((route) => route.name === "archive-group-detail");
if (!detailRoute || detailRoute.path !== "/archive/groups/:id/detail") {
  throw new Error("archive-group-detail route is missing");
}
```

Expected smoke failure: route does not exist yet.

- [ ] **Step 2: Add overview types and API function**

```ts
export interface ArchiveGroupItemStats {
  totalCount: number;
  enabledCount: number;
  disabledCount: number;
  idTypeCount: number;
  timeTypeCount: number;
}

export interface ArchiveGroupTaskStats {
  totalCount: number;
  successCount: number;
  failedCount: number;
  runningCount: number;
  lastExecuteStatus?: number;
  lastExecuteTime?: string;
}

export interface ArchiveGroupOverview {
  group: ArchiveGroup;
  itemStats: ArchiveGroupItemStats;
  taskStats: ArchiveGroupTaskStats;
  recentTasks: TaskItem[];
}

export function getArchiveGroupOverviewApi(id: number): Promise<ArchiveGroupOverview> {
  return http.get<ArchiveGroupOverview>(`/archive/groups/${id}/overview`);
}
```

- [ ] **Step 3: Add the detail route**

```ts
{
  path: "archive/groups/:id/detail",
  name: "archive-group-detail",
  component: ArchiveGroupDetailView
}
```

Also import the new view:

```ts
import ArchiveGroupDetailView from "../views/ArchiveGroupDetailView.vue";
```

- [ ] **Step 4: Add i18n keys used by the new page**

```ts
archiveGroupDetail: {
  title: "归档分组详情",
  summary: "执行概览",
  recentTasks: "最近执行任务",
  openDetail: "明细",
  viewTask: "查看任务详情",
  notFound: "归档分组不存在或已被删除",
  emptyTasks: "暂无执行记录"
}
```

- [ ] **Step 5: Run the frontend smoke check**

Run:

```bash
cd easyarchive-ui && npm run smoke-check
```

Expected: PASS for route/API registration checks.

- [ ] **Step 6: Commit the route/API slice**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts easyarchive-ui/src/router/index.ts easyarchive-ui/src/i18n/messages.ts easyarchive-ui/scripts/smoke-check.mjs
git commit -m "feat: add archive group detail route"
```

## Task 4: Add Internal Tab Host in the App Layout

**Files:**
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/styles/theme.css`

- [ ] **Step 1: Add a failing interaction check for duplicate detail tabs**

If the current smoke script can render the layout, add a check for de-duplication logic; otherwise add a focused unit-level helper in the component and assert it:

```ts
const next = openTab(
  [{ key: "archive-group-detail:10", title: "归档分组详情 - Order Archive", to: "/archive/groups/10/detail" }],
  { key: "archive-group-detail:10", title: "归档分组详情 - Order Archive", to: "/archive/groups/10/detail" }
);
expect(next).toHaveLength(1);
```

- [ ] **Step 2: Add tab state and route synchronization in `AppLayout.vue`**

Use a small local tab model:

```ts
type WorkTab = {
  key: string;
  title: string;
  to: string;
  closable: boolean;
};

const tabs = ref<WorkTab[]>([]);
const activeTabKey = ref("");

function openWorkTab(tab: WorkTab): void {
  const existing = tabs.value.find((item) => item.key === tab.key);
  if (!existing) {
    tabs.value.push(tab);
  }
  activeTabKey.value = tab.key;
}

function closeWorkTab(key: string): void {
  const index = tabs.value.findIndex((item) => item.key === key);
  if (index === -1) {
    return;
  }
  const wasActive = activeTabKey.value === key;
  tabs.value.splice(index, 1);
  if (wasActive) {
    const fallback = tabs.value[index - 1] || tabs.value[index] || null;
    if (fallback) {
      activeTabKey.value = fallback.key;
      void router.push(fallback.to);
    } else {
      activeTabKey.value = "";
    }
  }
}
```

Watch the current route and open/update detail tabs:

```ts
watch(
  () => router.currentRoute.value,
  (route) => {
    if (route.name === "archive-group-detail") {
      const id = String(route.params.id);
      openWorkTab({
        key: `archive-group-detail:${id}`,
        title: typeof route.query.title === "string" ? route.query.title : "归档分组详情",
        to: route.fullPath,
        closable: true
      });
    }
  },
  { immediate: true }
);
```

- [ ] **Step 3: Render the tab strip above `router-view`**

```vue
<div v-if="tabs.length" class="work-tabs">
  <button
    v-for="tab in tabs"
    :key="tab.key"
    class="work-tabs__item"
    :class="{ 'work-tabs__item--active': tab.key === activeTabKey }"
    @click="router.push(tab.to)"
  >
    <span>{{ tab.title }}</span>
    <span class="work-tabs__close" @click.stop="closeWorkTab(tab.key)">×</span>
  </button>
</div>
<main class="app-shell__content">
  <router-view />
</main>
```

- [ ] **Step 4: Add focused styles**

```css
.work-tabs {
  display: flex;
  gap: 8px;
  padding: 12px 16px 0;
  overflow-x: auto;
}

.work-tabs__item {
  border: 1px solid var(--border-color);
  background: var(--panel-bg);
  border-radius: 12px 12px 0 0;
  padding: 10px 12px;
}

.work-tabs__item--active {
  border-bottom-color: transparent;
  background: var(--surface-bg);
}
```

- [ ] **Step 5: Run the frontend smoke check**

Run:

```bash
cd easyarchive-ui && npm run smoke-check
```

Expected: PASS, with no duplicate tab creation for the same detail route.

- [ ] **Step 6: Commit the tab host slice**

```bash
git add easyarchive-ui/src/layouts/AppLayout.vue easyarchive-ui/src/styles/theme.css easyarchive-ui/scripts/smoke-check.mjs
git commit -m "feat: add internal work tabs"
```

## Task 5: Build the Archive Group Detail View

**Files:**
- Create: `easyarchive-ui/src/views/ArchiveGroupDetailView.vue`
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Modify: `easyarchive-ui/src/styles/theme.css`

- [ ] **Step 1: Add a failing render check for the detail page**

At minimum, assert the detail view renders the expected title area and handles not-found:

```ts
expect(screen.getByText("归档分组详情")).toBeInTheDocument();
expect(screen.getByText("执行概览")).toBeInTheDocument();
```

If the current project does not have component test infrastructure, use the smoke script to navigate to a mocked route and assert these labels in the rendered DOM.

- [ ] **Step 2: Implement the detail view data model and loader**

```ts
const route = useRoute();
const router = useRouter();
const loading = ref(false);
const loadError = ref("");
const overview = ref<ArchiveGroupOverview | null>(null);
const items = ref<ArchiveGroupItemSummary[]>([]);

const groupId = computed(() => Number(route.params.id));
const group = computed(() => overview.value?.group ?? null);

async function loadPage(): Promise<void> {
  loading.value = true;
  loadError.value = "";
  try {
    const [groupResult, itemResult, overviewResult] = await Promise.all([
      getArchiveGroupApi(groupId.value),
      getArchiveGroupItemsApi(groupId.value),
      getArchiveGroupOverviewApi(groupId.value)
    ]);

    overview.value = {
      ...overviewResult,
      group: overviewResult.group ?? groupResult
    };
    items.value = itemResult;
    updateTitle();
  } catch (error) {
    loadError.value = error instanceof Error ? error.message : t("archiveGroupDetail.notFound");
  } finally {
    loading.value = false;
  }
}

function updateTitle(): void {
  const title = group.value?.groupName
    ? `归档分组详情 - ${group.value.groupName}`
    : "归档分组详情";
  void router.replace({ query: { ...route.query, title } });
}
```

- [ ] **Step 3: Render the five planned sections**

Use this page structure:

```vue
<section class="detail-page">
  <header class="detail-page__header">
    <div>
      <h1>{{ group?.groupName || t("archiveGroupDetail.title") }}</h1>
      <p>{{ group?.groupCode }}</p>
    </div>
    <div class="detail-page__actions">
      <button class="btn btn--secondary" @click="openEditGroup">编辑分组</button>
      <button class="btn" :disabled="!group?.canTrigger" @click="triggerGroup">立即执行</button>
      <button class="btn btn--subtle" :disabled="!group?.activeTaskId" @click="viewTask">查看运行中任务</button>
    </div>
  </header>

  <section class="panel detail-page__meta">...</section>
  <section class="panel detail-page__items">...</section>
  <section class="panel detail-page__summary">...</section>
  <section class="panel detail-page__tasks">...</section>
</section>
```

Render item columns in this order:
- `优先级`
- `明细类型`
- `源表`
- `目标表`
- `步长`
- `写入`
- `清理`
- `状态`
- `操作`

Render task summary cards:
- `累计执行次数`
- `成功次数`
- `失败次数`
- `运行中次数`
- `最近执行结果`
- `最近执行时间`

- [ ] **Step 4: Wire list-page “明细” action to the detail route**

In `ArchiveGroupView.vue`, replace the current local-detail behavior with route navigation:

```ts
function openGroupDetail(group: ArchiveGroup): void {
  void router.push({
    name: "archive-group-detail",
    params: { id: group.id },
    query: { title: `归档分组详情 - ${group.groupName}` }
  });
}
```

Bind the button:

```vue
<button class="btn btn--subtle" @click="openGroupDetail(group)">
  {{ t("archiveGroupDetail.openDetail") }}
</button>
```

- [ ] **Step 5: Add styles for cards, summary metrics, and tables**

```css
.detail-page {
  display: grid;
  gap: 16px;
}

.detail-page__header,
.detail-page__summary-grid {
  display: grid;
  gap: 12px;
}

.detail-metric {
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(10, 88, 202, 0.06), rgba(10, 88, 202, 0.01));
}
```

- [ ] **Step 6: Run the frontend smoke check**

Run:

```bash
cd easyarchive-ui && npm run smoke-check
```

Expected: PASS, and the detail route renders the planned sections.

- [ ] **Step 7: Commit the detail page slice**

```bash
git add easyarchive-ui/src/views/ArchiveGroupDetailView.vue easyarchive-ui/src/views/ArchiveGroupView.vue easyarchive-ui/src/styles/theme.css easyarchive-ui/scripts/smoke-check.mjs
git commit -m "feat: add archive group detail workspace"
```

## Task 6: End-to-End Verification and Cleanup

**Files:**
- Modify only if needed after verification: files touched in Tasks 1-5

- [ ] **Step 1: Run targeted backend tests together**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupControllerContractTest,ArchiveGroupServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: PASS.

If this fails due to unrelated module wiring or stale compilation, retry with:

```bash
mvn test -pl easyarchive-starter -am -Dtest=ArchiveGroupControllerContractTest,ArchiveGroupServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```

- [ ] **Step 2: Run frontend smoke verification**

Run:

```bash
cd easyarchive-ui && npm run smoke-check
```

Expected: PASS.

- [ ] **Step 3: Run a manual browser verification**

Open the app and verify:

1. Clicking “明细” on one group opens `/archive/groups/:id/detail`.
2. Clicking “明细” again on the same group focuses the existing tab instead of duplicating it.
3. The page shows group meta, merged items, summary stats, and recent tasks.
4. Opening a second group creates a second internal tab.
5. Closing an active tab routes to a sensible fallback.

If a local dev server exists, use the in-app browser rather than external tooling.

- [ ] **Step 4: Inspect the final diff**

Run:

```bash
git diff --stat
git diff -- easyarchive-starter easyarchive-ui
```

Expected: only the planned backend/frontend/detail-tab changes are present; no unrelated revert.

- [ ] **Step 5: Create the final implementation commit or commits**

If the earlier task commits were used, this step may be a no-op. Otherwise create clean commits grouped by backend and frontend slices:

```bash
git add easyarchive-starter easyarchive-ui
git commit -m "feat: add archive group detail tab"
```

