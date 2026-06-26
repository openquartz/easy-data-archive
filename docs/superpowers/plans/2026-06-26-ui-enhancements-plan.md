# 归档平台界面增强与接口扩展 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 优化归档分组界面筛选布局，增强归档任务列表的列和筛选能力，为归档连接列表增加筛选和分页功能。

**Architecture:** 后端在现有 Controller/Service/Mapper 层递增修改，新增分页接口和筛选参数；前端将筛选条件统一到工具栏布局，新增列和下拉筛选器。前后端通过 REST API 参数扩展交互。

**Tech Stack:** Java 11 + Spring Boot 2.3.2 + MyBatis + Vue 3 + TypeScript

---

### Task 1: 后端 - 任务列表接口增加 groupId 参数支持

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java:21-28`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskLogService.java:8`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java:43-62`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/repository/ArchiveLogRepository.java:17-19`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/repository/JdbcArchiveLogRepository.java:42-50`

- [ ] **Step 1: 修改 ArchiveTaskLogController 增加 groupId 参数**

```java
@GetMapping("/tasks")
public ApiResponse<Map<String, Object>> getTasks(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long groupId) {
    page = Math.max(1, page);
    size = Math.min(Math.max(1, size), 500);
    return ApiResponse.success(taskLogService.queryTasks(page, size, status, groupId));
}
```

- [ ] **Step 2: 修改 ArchiveTaskLogService 接口**

```java
Map<String, Object> queryTasks(int page, int size, String status, Long groupId);
```

- [ ] **Step 3: 修改 ArchiveTaskLogServiceImpl 传递 groupId**

```java
@Override
public Map<String, Object> queryTasks(int page, int size, String status, Long groupId) {
    List<ArchiveGroupExecuteTask> list;
    int total;
    CurrentUserInfo currentUser = currentUserService.getCurrentUser();
    if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
        list = archiveLogRepository.queryTasks(page, size, status, groupId);
        total = archiveLogRepository.countTasks(status, groupId);
    } else {
        int offset = (page - 1) * size;
        Long userId = currentUser.getUserId();
        list = archiveGroupExecuteTaskMapper.selectPageByUser(userId, offset, size, status, groupId);
        total = archiveGroupExecuteTaskMapper.countByUser(userId, status, groupId);
    }
    Map<String, Object> result = new HashMap<>();
    result.put("list", TaskConvertUtils.fromEntityTaskList(list));
    result.put("total", total);
    result.put("page", page);
    result.put("size", size);
    return result;
}
```

- [ ] **Step 4: 修改 ArchiveLogRepository 接口**

```java
List<ArchiveGroupExecuteTask> queryTasks(int page, int size, String status, Long groupId);
int countTasks(String status, Long groupId);
```

- [ ] **Step 5: 修改 JdbcArchiveLogRepository 实现**

```java
@Override
public List<ArchiveGroupExecuteTask> queryTasks(int page, int size, String status, Long groupId) {
    int offset = (page - 1) * size;
    return executeTaskMapper.selectPage(offset, size, status, groupId);
}

@Override
public int countTasks(String status, Long groupId) {
    return executeTaskMapper.count(status, groupId);
}
```

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl easyarchive-starter,easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveTaskLogController.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveTaskLogService.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveTaskLogServiceImpl.java \
        easyarchive-core/src/main/java/com/openquartz/easyarchive/core/repository/ArchiveLogRepository.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/repository/JdbcArchiveLogRepository.java
git commit -m "feat: task list API supports groupId filter parameter"
```

---

### Task 2: 后端 - TaskVO 增加 groupName 字段 & SQL 支持

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/TaskVO.java:16`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java:21-32`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml:1-28`

- [ ] **Step 1: TaskVO 增加 groupName 字段**

在 `private Long groupId;` 之后增加：

```java
private String groupName;
```

- [ ] **Step 2: 更新 BaseResultMap 增加 groupName 映射**

```xml
<result column="group_name" property="groupName"/>
```

同时更新 Base_Column_List 增加 `group_name`。

- [ ] **Step 3: 修改 selectPage 和 count 支持 groupId**

```xml
<select id="selectPage" resultMap="BaseResultMap">
    SELECT t.id, t.group_id, g.group_name, t.start_time, t.end_time, t.execute_status, t.error_msg,
           t.processed_records, t.processed_speed, t.heartbeat_time, t.finished_flag,
           t.created_time, t.updated_time, t.creator_id, t.updater_id, t.deleted
    FROM ea_archive_group_execute_task t
    JOIN ea_archive_group g ON g.id = t.group_id AND g.deleted = 0
    <where>
        <if test="status != null and status != ''">AND t.execute_status = #{status}</if>
        <if test="groupId != null">AND t.group_id = #{groupId}</if>
        AND t.deleted = 0
    </where>
    ORDER BY t.id DESC
    LIMIT #{offset}, #{size}
</select>

<select id="count" resultType="int">
    SELECT COUNT(1)
    FROM ea_archive_group_execute_task t
    JOIN ea_archive_group g ON g.id = t.group_id AND g.deleted = 0
    <where>
        <if test="status != null and status != ''">AND t.execute_status = #{status}</if>
        <if test="groupId != null">AND t.group_id = #{groupId}</if>
        AND t.deleted = 0
    </where>
</select>
```

- [ ] **Step 4: 修改 selectPageByUser 和 countByUser**

```xml
<select id="selectPageByUser" resultMap="BaseResultMap">
    SELECT t.id, t.group_id, g.group_name, t.start_time, t.end_time, t.execute_status, t.error_msg,
           t.processed_records, t.processed_speed, t.heartbeat_time, t.finished_flag,
           t.created_time, t.updated_time, t.creator_id, t.updater_id, t.deleted
    FROM ea_archive_group_execute_task t
    JOIN ea_archive_group g ON g.id = t.group_id AND g.deleted = 0
    <where>
        <if test="status != null and status != ''">AND t.execute_status = #{status}</if>
        <if test="groupId != null">AND t.group_id = #{groupId}</if>
        AND t.deleted = 0
        AND EXISTS (
            SELECT 1 FROM ea_user_datasource_permission p
            WHERE p.deleted = 0 AND p.user_id = #{userId}
              AND p.datasource_id = g.source_datasource_id AND p.permission_type = 'READ'
        )
    </where>
    ORDER BY t.id DESC
    LIMIT #{offset}, #{size}
</select>

<select id="countByUser" resultType="int">
    SELECT COUNT(1)
    FROM ea_archive_group_execute_task t
    JOIN ea_archive_group g ON g.id = t.group_id AND g.deleted = 0
    <where>
        <if test="status != null and status != ''">AND t.execute_status = #{status}</if>
        <if test="groupId != null">AND t.group_id = #{groupId}</if>
        AND t.deleted = 0
        AND EXISTS (
            SELECT 1 FROM ea_user_datasource_permission p
            WHERE p.deleted = 0 AND p.user_id = #{userId}
              AND p.datasource_id = g.source_datasource_id AND p.permission_type = 'READ'
        )
    </where>
</select>
```

- [ ] **Step 5: ArchiveGroupExecuteTaskMapper 接口增加对应方法**

在接口中增加带 `groupId` 参数的 `selectPage`、`count`、`selectPageByUser`、`countByUser` 重载方法（使用 `@Param`）。

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl easyarchive-starter,easyarchive-core -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/TaskVO.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java \
        easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml
git commit -m "feat: task VO adds groupName, SQL supports group filter with JOIN"
```

---

### Task 3: 后端 - 归档连接分页查询接口

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionController.java:36-41`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveConnectionService.java:15`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java:53-65`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveConnectionMapper.java:139-151`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveConnectionMapper.xml:139-160`

- [ ] **Step 1: ArchiveConnectionService 增加 findPage 方法**

```java
PageResult<ArchiveConnection> findPage(int page, int size, String keyword, Integer status);
```

- [ ] **Step 2: ArchiveConnectionServiceImpl 实现 findPage**

```java
@Override
public PageResult<ArchiveConnection> findPage(int page, int size, String keyword, Integer status) {
    CurrentUserInfo currentUser = currentUserService.getCurrentUser();
    int start = (page - 1) * size;
    List<ArchiveConnection> list;
    long total;
    if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
        total = datasourceMapper.countByKeyword(keyword, status);
        list = datasourceMapper.selectByKeyword(keyword, status, start, size);
    } else {
        Long userId = currentUser.getUserId();
        Set<Long> ids = datasourceAuthorizationService.listDatasourceIdsByLevel(userId,
                RoleConstants.isArchiveAdmin(currentUser.getRoleCode())
                        ? DatasourcePermissionLevelEnum.MANAGE : DatasourcePermissionLevelEnum.USE);
        if (ids.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, page, size);
        }
        List<ArchiveConnection> authorized = datasourceMapper.selectAuthorizedListByIds(ids);
        List<ArchiveConnection> filtered = authorized.stream()
                .filter(d -> keyword == null || keyword.isEmpty()
                        || d.getDatasourceName().contains(keyword)
                        || d.getDatasourceCode().contains(keyword))
                .filter(d -> status == null || status.equals(d.getStatus()))
                .collect(Collectors.toList());
        total = filtered.size();
        int end = Math.min(start + size, filtered.size());
        list = start >= filtered.size() ? Collections.emptyList() : filtered.subList(start, end);
    }
    return PageResult.of(maskPasswords(list), total, page, size);
}
```

- [ ] **Step 3: ArchiveConnectionMapper 增加方法**

```java
long countByKeyword(@Param("keyword") String keyword, @Param("status") Integer status);
List<ArchiveConnection> selectByKeyword(@Param("keyword") String keyword,
                                        @Param("status") Integer status,
                                        @Param("start") int start,
                                        @Param("size") int size);
```

- [ ] **Step 4: ArchiveConnectionMapper.xml 增加 SQL**

```xml
<select id="countByKeyword" resultType="int">
    SELECT COUNT(1) FROM ea_archive_datasource
    <where>
        <if test="status != null">AND status = #{status}</if>
        <if test="keyword != null and keyword != ''">
            AND (datasource_name LIKE CONCAT('%', #{keyword}, '%')
                 OR datasource_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        AND deleted = 0
    </where>
</select>

<select id="selectByKeyword" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/> FROM ea_archive_datasource
    <where>
        <if test="status != null">AND status = #{status}</if>
        <if test="keyword != null and keyword != ''">
            AND (datasource_name LIKE CONCAT('%', #{keyword}, '%')
                 OR datasource_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        AND deleted = 0
    </where>
    ORDER BY id DESC
    LIMIT #{start}, #{size}
</select>
```

- [ ] **Step 5: ArchiveConnectionController 增加分页接口**

```java
@GetMapping("/page")
public ApiResponse<Map<String, Object>> getDatasourcesPage(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status) {
    page = Math.max(1, page);
    size = Math.min(Math.max(1, size), 500);
    PageResult<ArchiveConnection> result = datasourceService.findPage(page, size, keyword, status);
    List<DatasourceVO> voList = result.getData().stream()
            .map(datasourceConverter::toVO)
            .collect(Collectors.toList());
    Map<String, Object> response = new java.util.HashMap<>();
    response.put("data", voList);
    response.put("total", result.getTotal());
    response.put("page", result.getPage());
    response.put("size", result.getSize());
    return ApiResponse.success(response);
}
```

- [ ] **Step 6: 编译验证**

Run: `mvn compile -pl easyarchive-starter -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: 提交**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveConnectionController.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveConnectionService.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveConnectionServiceImpl.java \
        easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveConnectionMapper.java \
        easyarchive-starter/src/main/resources/mapper/ArchiveConnectionMapper.xml
git commit -m "feat: datasource list adds paginated query API with keyword and status filter"
```

---

### Task 4: 前端 - task API 层增加 groupName 和 groupId

**Files:**
- Modify: `easyarchive-ui/src/api/task.ts:3-16`
- Modify: `easyarchive-ui/src/api/task.ts:39-54`

- [ ] **Step 1: TaskItem 增加 groupName 字段**

```typescript
export interface TaskItem {
  id: number;
  groupId: number;
  groupName?: string;
  startTime?: string;
  endTime?: string;
  executeStatus: number;
  errorMsg?: string;
  processedRecords?: number;
  processedSpeed?: number;
  heartbeatTime?: string;
  finishedFlag?: number;
  createdTime?: string;
  updatedTime?: string;
}
```

- [ ] **Step 2: TaskQuery 增加 groupId 字段**

```typescript
export interface TaskQuery {
  page: number;
  size: number;
  status?: string;
  groupId?: number;
}
```

- [ ] **Step 3: getTasksApi 支持传递 groupId**

```typescript
export function getTasksApi(query: TaskQuery): Promise<PagedResult<TaskItem>> {
  const params = new URLSearchParams();
  params.set("page", String(query.page));
  params.set("size", String(query.size));
  if (query.status) params.set("status", query.status);
  if (query.groupId != null) params.set("groupId", String(query.groupId));
  return http.get<PagedResult<TaskItem>>(`/task-log/tasks?${params.toString()}`);
}
```

- [ ] **Step 4: 验证 TypeScript 编译**

Run: `cd easyarchive-ui && npx vue-tsc --noEmit 2>&1 | head -20`
Expected: 无新增错误

- [ ] **Step 5: 提交**

```bash
git add easyarchive-ui/src/api/task.ts
git commit -m "feat: task API adds groupName field and groupId filter parameter"
```

---

### Task 5: 前端 - datasource API 层改为分页调用

**Files:**
- Modify: `easyarchive-ui/src/api/datasource.ts:41`

- [ ] **Step 1: 增加 PageResult 接口和分页函数**

```typescript
export interface PageResult<T> {
  data: T[];
  total: number;
  page: number;
  size: number;
}

export interface DatasourcePageParams {
  page: number;
  size: number;
  keyword?: string;
  status?: number;
}

export function getDatasourcesPageApi(params: DatasourcePageParams): Promise<PageResult<Datasource>> {
  const query = new URLSearchParams();
  if (params.page != null) query.set("page", String(params.page));
  if (params.size != null) query.set("size", String(params.size));
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.status !== undefined) query.set("status", String(params.status));
  return http.get<PageResult<Datasource>>(`/archive/datasources/page?${query.toString()}`);
}
```

- [ ] **Step 2: 验证 TypeScript 编译**

Run: `cd easyarchive-ui && npx vue-tsc --noEmit 2>&1 | head -20`
Expected: 无新增错误

- [ ] **Step 3: 提交**

```bash
git add easyarchive-ui/src/api/datasource.ts
git commit -m "feat: datasource API adds paginated query function"
```

---

### Task 6: 前端 - i18n 增加新增文案

**Files:**
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: 在 zh-CN 和 en-US 中增加 datasource.filters 和 task 新增文案**

zh-CN 新增：
- `datasource.filters.keyword`: "搜索"
- `datasource.filters.keywordPlaceholder`: "连接名称或编码"
- `datasource.filters.statusAll`: "全部"
- `datasource.filters.search`: "查询"
- `datasource.filters.reset`: "重置"
- `task.filters.group`: "归档分组"
- `task.filters.groupAll`: "全部"
- `task.columns.groupName`: "归档分组名称"

en-US 新增对应英文：
- `datasource.filters.keyword`: "Search"
- `datasource.filters.keywordPlaceholder`: "Connection name or code"
- `datasource.filters.statusAll`: "All"
- `datasource.filters.search`: "Search"
- `datasource.filters.reset`: "Reset"
- `task.filters.group`: "Archive Group"
- `task.filters.groupAll`: "All"
- `task.columns.groupName`: "Archive Group Name"

- [ ] **Step 2: 验证编译并提交**

```bash
git add easyarchive-ui/src/i18n/messages.ts
git commit -m "feat: i18n adds datasource filter and task group column labels"
```

---

### Task 7: 前端 - 归档分组界面优化筛选条件框展示

**Files:**
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue:289-309` (template filter-bar 区域)
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue:280-288` (toolbar 区域)

- [ ] **Step 1: 将筛选控件从独立 filter-bar 迁移到工具栏右侧**

修改 template 结构，将 `.filter-bar` 中的控件移入 `.page-toolbar .actions` 中：

```html
<header class="page-toolbar">
  <h1>{{ t("archiveGroup.title") }}</h1>
  <div class="actions">
    <input
      v-model="filters.keyword"
      type="text"
      :placeholder="t('archiveGroup.filters.keywordPlaceholder')"
      class="filter-bar__input"
    />
    <select v-model.number="filters.enableStatus" class="filter-bar__select">
      <option :value="undefined">{{ t("archiveGroup.filters.statusAll") }}</option>
      <option :value="0">{{ t("status.enabled") }}</option>
      <option :value="1">{{ t("status.disabled") }}</option>
    </select>
    <select v-model.number="filters.ownerUserId" class="filter-bar__select">
      <option :value="undefined">{{ t("archiveGroup.filters.ownerAll") }}</option>
      <option v-for="user in users" :key="user.id" :value="user.id">
        {{ user.realName || user.username }}
      </option>
    </select>
    <button class="btn btn--subtle" @click="handleSearch">{{ t("archiveGroup.filters.search") }}</button>
    <button v-if="hasActiveFilter" class="btn btn--subtle" @click="handleReset">{{ t("archiveGroup.filters.reset") }}</button>
    <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
    <button v-if="authStore.hasCapability('ARCHIVE_GROUP_CREATE')" class="btn btn--primary" :disabled="loading" @click="openCreateGroup">{{ t("archiveGroup.new") }}</button>
  </div>
</header>
```

- [ ] **Step 2: 删除原有的独立 filter-bar div**

移除以下模板代码：

```html
<div class="filter-bar">
  ...原有筛选控件...
</div>
```

- [ ] **Step 3: 验证编译并提交**

```bash
git add easyarchive-ui/src/views/ArchiveGroupView.vue
git commit -m "feat: archive group filter controls moved to toolbar (consistent layout)"
```

---

### Task 8: 前端 - 归档任务列表增加分组名称列和分组筛选

**Files:**
- Modify: `easyarchive-ui/src/views/TaskListView.vue:1-117` (script)
- Modify: `easyarchive-ui/src/views/TaskListView.vue:119-183` (template)

- [ ] **Step 1: Script 中引入归档分组 API 并增加分组筛选状态**

```typescript
import { getArchiveGroupsApi, type ArchiveGroup } from "../api/archiveGroup";

// 在现有 refs 之后增加：
const groups = ref<ArchiveGroup[]>([]);
const groupFilter = ref<number | undefined>(undefined);
```

- [ ] **Step 2: 修改 loadData 加载归档分组列表并传递 groupId**

```typescript
async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [result, groupsResult] = await Promise.all([
      getTasksApi({ page: page.value, size: size.value, status: statusFilter.value || undefined, groupId: groupFilter.value }),
      getArchiveGroupsApi().catch(() => [] as ArchiveGroup[])
    ]);
    list.value = result.list || [];
    total.value = result.total || 0;
    groups.value = groupsResult;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("task.loadFailed");
  } finally {
    loading.value = false;
  }
}
```

- [ ] **Step 3: 修改 applyFilter 和 refresh 函数**

```typescript
function applyFilter(): void {
  page.value = 1;
  void loadData();
}

function resetFilters(): void {
  statusFilter.value = "";
  groupFilter.value = undefined;
  page.value = 1;
  void loadData();
}
```

- [ ] **Step 4: 修改 template - 工具栏增加归档分组下拉**

```html
<header class="page-toolbar">
  <h1>{{ t("task.title") }}</h1>
  <div class="actions">
    <select v-model.number="groupFilter" :disabled="loading" @change="applyFilter">
      <option :value="undefined">{{ t("task.filters.groupAll") }}</option>
      <option v-for="group in groups" :key="group.id" :value="group.id">
        {{ group.groupName || group.groupCode }}
      </option>
    </select>
    <select v-model="statusFilter" :disabled="loading" @change="applyFilter">
      <option v-for="item in statusOptions" :key="item.value || 'all'" :value="item.value">
        {{ item.label }}
      </option>
    </select>
    <button class="btn btn--subtle" :disabled="loading" @click="refresh">{{ t("common.refresh") }}</button>
  </div>
</header>
```

- [ ] **Step 5: 表格增加归档分组名称列**

```html
<th>{{ t("task.columns.id") }}</th>
<th>{{ t("task.columns.groupName") }}</th>
<th>{{ t("task.columns.groupId") }}</th>
<th>{{ t("task.columns.status") }}</th>
```

tbody 中：

```html
<td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
<td>{{ item.groupName || "-" }}</td>
<td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
```

- [ ] **Step 6: 验证编译并提交**

Run: `cd easyarchive-ui && npx vue-tsc --noEmit 2>&1 | head -20`

```bash
git add easyarchive-ui/src/views/TaskListView.vue
git commit -m "feat: task list adds group name column and archive group dropdown filter"
```

---

### Task 9: 前端 - 归档连接列表增加筛选和分页

**Files:**
- Modify: `easyarchive-ui/src/views/DatasourceView.vue:1-152` (script)
- Modify: `easyarchive-ui/src/views/DatasourceView.vue:154-216` (template)

- [ ] **Step 1: Script 中引入分页 API 和状态管理**

将 import 改为：

```typescript
import {
  createDatasourceApi,
  type Datasource,
  type DatasourcePayload,
  type DatasourceTypeOption,
  getDatasourcesPageApi,
  getDatasourceTypesApi,
  testDatasourceConnectionApi,
  updateDatasourceApi,
  updateDatasourceStatusApi
} from "../api/datasource";
```

增加分页和筛选状态：

```typescript
const page = ref(1);
const size = ref(20);
const total = ref(0);
const keywordFilter = ref("");
const statusFilter = ref<number | undefined>(undefined);
```

- [ ] **Step 2: 修改 loadData 使用分页接口**

```typescript
async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [datasourceResult, datasourceTypeList] = await Promise.all([
      getDatasourcesPageApi({
        page: page.value,
        size: size.value,
        keyword: keywordFilter.value || undefined,
        status: statusFilter.value
      }),
      getDatasourceTypesApi()
    ]);
    list.value = datasourceResult.data;
    total.value = datasourceResult.total;
    datasourceTypes.value = datasourceTypeList;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("datasource.loadFailed");
  } finally {
    loading.value = false;
  }
}
```

增加分页和重置函数：

```typescript
function handleSearch(): void {
  page.value = 1;
  void loadData();
}

function handleReset(): void {
  keywordFilter.value = "";
  statusFilter.value = undefined;
  page.value = 1;
  void loadData();
}

function handlePageChange(newPage: number): void {
  page.value = newPage;
  void loadData();
}

function handleSizeChange(newSize: number): void {
  size.value = newSize;
  page.value = 1;
  void loadData();
}

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));
```

- [ ] **Step 3: 修改 template - 工具栏增加筛选控件**

```html
<header class="page-toolbar">
  <h1>{{ t("datasource.title") }}</h1>
  <div class="actions">
    <input
      v-model="keywordFilter"
      type="text"
      :placeholder="t('datasource.filters.keywordPlaceholder')"
      class="filter-bar__input"
    />
    <select v-model.number="statusFilter">
      <option :value="undefined">{{ t("datasource.filters.statusAll") }}</option>
      <option :value="0">{{ t("status.untested") }}</option>
      <option :value="1">{{ t("status.enabled") }}</option>
      <option :value="2">{{ t("status.disabled") }}</option>
    </select>
    <button class="btn btn--subtle" :disabled="loading" @click="handleSearch">{{ t("datasource.filters.search") }}</button>
    <button class="btn btn--subtle" :disabled="loading" @click="handleReset">{{ t("datasource.filters.reset") }}</button>
    <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
    <button v-if="authStore.hasCapability('DATASOURCE_CREATE')" class="btn btn--primary" :disabled="loading" @click="openCreate">{{ t("datasource.new") }}</button>
  </div>
</header>
```

- [ ] **Step 4: 模板增加分页控件**

在表格之后、DatasourceFormDialog 之前增加：

```html
<div v-if="total > size" class="pager">
  <div class="pager__info">
    {{ t("datasource.pager", { page: page, totalPages, total }) }}
  </div>
  <div class="pager__controls">
    <button class="btn btn--subtle" :disabled="page <= 1 || loading" @click="handlePageChange(page - 1)">
      {{ t("common.prev") }}
    </button>
    <button class="btn btn--subtle" :disabled="page >= totalPages || loading" @click="handlePageChange(page + 1)">
      {{ t("common.next") }}
    </button>
    <select :value="size" class="pager__size" @change="handleSizeChange(Number(($event.target as HTMLSelectElement).value))">
      <option :value="10">10</option>
      <option :value="20">20</option>
      <option :value="50">50</option>
    </select>
  </div>
</div>
```

需要增加 i18n 文案 `datasource.pager`: "第 {page} / {totalPages} 页 · 共 {total} 条"

- [ ] **Step 5: 删除原有的 hint 段落（被筛选区域替代）**

移除 `<p class="hint">{{ t("datasource.connectionTip") }}</p>` 或保留在表格下方作为辅助提示。

- [ ] **Step 6: 验证编译并提交**

Run: `cd easyarchive-ui && npx vue-tsc --noEmit 2>&1 | head -20`

```bash
git add easyarchive-ui/src/views/DatasourceView.vue
git commit -m "feat: datasource list adds filter bar and pagination controls"
```

---

### Task 10: 最终验证

- [ ] **Step 1: 后端全量编译**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: 前端 TypeScript 检查**

Run: `cd easyarchive-ui && npx vue-tsc --noEmit`
Expected: 无新增错误

- [ ] **Step 3: 后端测试**

Run: `mvn test -pl easyarchive-starter -q`
Expected: 所有测试通过（或仅已有失败）

- [ ] **Step 4: 最终 git 确认**

Run: `git log --oneline -10`
Expected: 9 个清晰的 commit（Task 1-9 各一个）

---

## 变更总结

| 层 | 文件 | 变更类型 |
|---|---|---|
| 后端 Controller | `ArchiveTaskLogController.java` | 增加 `groupId` 参数 |
| 后端 Controller | `ArchiveConnectionController.java` | 新增 `/page` 分页接口 |
| 后端 Service | `ArchiveTaskLogService.java` | 接口增加 `groupId` |
| 后端 Service | `ArchiveConnectionService.java` | 接口增加 `findPage` |
| 后端 Service Impl | `ArchiveTaskLogServiceImpl.java` | 实现 `groupId` 传递 |
| 后端 Service Impl | `ArchiveConnectionServiceImpl.java` | 实现分页+筛选逻辑 |
| 后端 Mapper | `ArchiveGroupExecuteTaskMapper.java` | 重载方法支持 `groupId` |
| 后端 Mapper | `ArchiveConnectionMapper.java` | 新增 `countByKeyword`/`selectByKeyword` |
| 后端 Mapper XML | `ArchiveGroupExecuteTaskMapper.xml` | SQL 增加 JOIN + groupId 条件 |
| 后端 Mapper XML | `ArchiveConnectionMapper.xml` | 新增分页筛选 SQL |
| 后端 VO | `TaskVO.java` | 增加 `groupName` 字段 |
| 后端 Repository | `ArchiveLogRepository.java` | 接口增加 `groupId` 参数 |
| 后端 Repository | `JdbcArchiveLogRepository.java` | 实现 `groupId` 传递 |
| 前端 API | `src/api/task.ts` | 增加 `groupName`、`groupId` 参数 |
| 前端 API | `src/api/datasource.ts` | 增加分页 API 函数 |
| 前端 View | `ArchiveGroupView.vue` | 筛选条件迁移到工具栏 |
| 前端 View | `TaskListView.vue` | 增加分组名称列和分组筛选 |
| 前端 View | `DatasourceView.vue` | 增加筛选和分页 |
| 前端 i18n | `src/i18n/messages.ts` | 新增中英文案 |
