# 归档分组管理界面 — 分页、筛选与按钮布局优化 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为归档分组管理界面增加分页、筛选功能，并优化操作列按钮防止重叠。

**Architecture:** 扩展已有 `/page` 后端接口支持 keyword/ownerUserId 参数，前端新增筛选工具栏和分页栏，操作列设置横向滚动。

**Tech Stack:** Java 11 + Spring Boot 2.3.2 + MyBatis + Vue 3 + TypeScript

---

## 文件变更清单

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml` | 修改 | 新增 `selectByKeyword`、`countByKeyword` SQL |
| `easyarchive-starter/src/main/java/.../mapper/ArchiveGroupMapper.java` | 修改 | 新增 `selectByKeyword`、`countByKeyword` 方法 |
| `easyarchive-starter/src/main/java/.../service/ArchiveGroupService.java` | 修改 | `findPage` 方法增加 `keyword`、`ownerUserId` 参数 |
| `easyarchive-starter/src/main/java/.../service/impl/ArchiveGroupServiceImpl.java` | 修改 | `findPage` 实现增加筛选逻辑 |
| `easyarchive-starter/src/main/java/.../controller/ArchiveGroupController.java` | 修改 | `/page` 端点增加 `@RequestParam` |
| `easyarchive-ui/src/api/archiveGroup.ts` | 修改 | 新增 `ArchiveGroupPageParams` 和 `getArchiveGroupsPageApi` |
| `easyarchive-ui/src/i18n/messages.ts` | 修改 | 新增筛选/分页 i18n 文本 |
| `easyarchive-ui/src/views/ArchiveGroupView.vue` | 修改 | 新增筛选工具栏、分页栏、操作列滚动 |
| `easyarchive-ui/src/styles/theme.css` | 修改 | 新增 `.row-actions` 滚动样式 |

---

### Task 1: 后端 — Mapper XML 新增 SQL

**Files:**
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml`

- [ ] **Step 1: 在 `</mapper>` 标签前新增 SQL 语句**

```xml
<!-- 新增：按关键词搜索的分页查询 -->
<select id="selectByKeyword" resultMap="BaseResultMap">
    SELECT <include refid="Base_Column_List"/>
    FROM ea_archive_group
    <where>
        <if test="enableStatus != null">
            AND enable_status = #{enableStatus}
        </if>
        <if test="keyword != null and keyword != ''">
            AND (group_name LIKE CONCAT('%', #{keyword}, '%')
                 OR group_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="ownerUserId != null">
            AND owner_user_id = #{ownerUserId}
        </if>
        AND deleted = 0
    </where>
    ORDER BY group_level ASC, parent_id ASC, id DESC
    LIMIT #{start}, #{size}
</select>

<!-- 新增：按关键词搜索的计数 -->
<select id="countByKeyword" resultType="int">
    SELECT COUNT(*)
    FROM ea_archive_group
    <where>
        <if test="enableStatus != null">
            AND enable_status = #{enableStatus}
        </if>
        <if test="keyword != null and keyword != ''">
            AND (group_name LIKE CONCAT('%', #{keyword}, '%')
                 OR group_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        <if test="ownerUserId != null">
            AND owner_user_id = #{ownerUserId}
        </if>
        AND deleted = 0
    </where>
</select>
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml
git commit -m "feat: ArchiveGroupMapper 新增按关键词搜索的分页查询和计数 SQL"
```

---

### Task 2: 后端 — Mapper 接口新增方法

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupMapper.java`

- [ ] **Step 1: 在接口中新增两个方法声明**

当前接口中有 `selectPage`、`count` 等方法。在末尾添加：

```java
/**
 * 按关键词和负责人筛选分页查询
 */
List<ArchiveGroup> selectByKeyword(
    @Param("keyword") String keyword,
    @Param("enableStatus") Integer enableStatus,
    @Param("ownerUserId") Long ownerUserId,
    @Param("start") int start,
    @Param("size") int size
);

/**
 * 按关键词和负责人筛选计数
 */
int countByKeyword(
    @Param("keyword") String keyword,
    @Param("enableStatus") Integer enableStatus,
    @Param("ownerUserId") Long ownerUserId
);
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupMapper.java
git commit -m "feat: ArchiveGroupMapper 接口新增 selectByKeyword 和 countByKeyword 方法"
```

---

### Task 3: 后端 — Service 接口扩展

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`

- [ ] **Step 1: 修改 `findPage` 方法签名，增加 `keyword` 和 `ownerUserId` 参数**

当前签名：
```java
PageResult<ArchiveGroupView> findPage(Integer enableStatus, int page, int size);
```

改为：
```java
PageResult<ArchiveGroupView> findPage(Integer enableStatus, int page, int size, String keyword, Long ownerUserId);
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java
git commit -m "feat: ArchiveGroupService.findPage 增加 keyword 和 ownerUserId 参数"
```

---

### Task 4: 后端 — Service 实现扩展

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`

- [ ] **Step 1: 修改 `findPage` 方法实现**

当前实现（第 124 行附近）有三条分支（admin / archiveAdmin / normal）。需要修改：
1. 方法签名增加 `String keyword, Long ownerUserId` 参数
2. admin 分支：调用 `groupMapper.countByKeyword` 和 `groupMapper.selectByKeyword`
3. archiveAdmin 和 normal 分支暂时保持不变（不应用 keyword 筛选，因为它们的查询已有额外权限过滤）

修改后的 admin 分支代码：

```java
if (RoleConstants.isAdmin(currentUser.getRoleCode())) {
    // 系统管理员 - 查看所有分组（支持关键词筛选）
    total = groupMapper.countByKeyword(keyword, enableStatus, ownerUserId);
    groups = groupMapper.selectByKeyword(keyword, enableStatus, ownerUserId, start, size);
} else if (RoleConstants.isArchiveAdmin(currentUser.getRoleCode())) {
    // 归档管理员...
    // (不变)
} else {
    // 普通用户...
    // (不变)
}
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java
git commit -m "feat: ArchiveGroupServiceImpl.findPage 实现关键词和负责人筛选"
```

---

### Task 5: 后端 — Controller 扩展

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`

- [ ] **Step 1: 修改 `/page` 端点，增加 `keyword` 和 `ownerUserId` 参数**

当前代码（第 50-56 行）：
```java
@GetMapping("/page")
public ApiResponse<PageResult<ArchiveGroupView>> page(
        @RequestParam(required = false) Integer enableStatus,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.success(groupService.findPage(enableStatus, page, size));
}
```

改为：
```java
@GetMapping("/page")
public ApiResponse<PageResult<ArchiveGroupView>> page(
        @RequestParam(required = false) Integer enableStatus,
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long ownerUserId) {
    return ApiResponse.success(groupService.findPage(enableStatus, page, size, keyword, ownerUserId));
}
```

注意：`size` 默认值从 `10` 改为 `20`，与设计确认一致。

- [ ] **Step 2: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java
git commit -m "feat: ArchiveGroupController /page 端点增加 keyword/ownerUserId 参数，默认 size 改为 20"
```

---

### Task 6: 前端 — API 层扩展

**Files:**
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`

- [ ] **Step 1: 新增 `PageResult` 接口和 `ArchiveGroupPageParams` 接口、`getArchiveGroupsPageApi` 函数**

在文件末尾（`cancelArchiveGroupActiveTaskApi` 之后）添加：

```typescript
export interface PageResult<T> {
  data: T[];
  total: number;
  page: number;
  size: number;
}

export interface ArchiveGroupPageParams {
  page?: number;
  size?: number;
  enableStatus?: number;
  keyword?: string;
  ownerUserId?: number;
}

export function getArchiveGroupsPageApi(params: ArchiveGroupPageParams): Promise<PageResult<ArchiveGroup>> {
  const query = new URLSearchParams();
  if (params.page != null) query.set("page", String(params.page));
  if (params.size != null) query.set("size", String(params.size));
  if (params.enableStatus !== undefined) query.set("enableStatus", String(params.enableStatus));
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.ownerUserId != null) query.set("ownerUserId", String(params.ownerUserId));
  return http.get<PageResult<ArchiveGroup>>(`/archive/groups/page?${query.toString()}`);
}
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts
git commit -m "feat: archiveGroup API 新增分页查询函数和参数接口"
```

---

### Task 7: 前端 — i18n 文本

**Files:**
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: 在 zh-CN `archiveGroup` 对象中新增 `filters` 和 `pager` 字段**

zh-CN 的 `archiveGroup` 从第 601 行开始。在现有 `loadFailed` 之后（约第 613 行）插入：

```typescript
filters: {
  keyword: "搜索",
  keywordPlaceholder: "分组名称或编码",
  status: "状态",
  statusAll: "全部",
  owner: "负责人",
  ownerAll: "全部",
  search: "查询",
  reset: "重置"
},
pager: "第 {page} / {totalPages} 页 · 共 {total} 条",
emptyFilter: "未找到匹配条件的归档分组，请尝试其他关键词或重置筛选"
```

- [ ] **Step 2: 在 en-US `archiveGroup` 对象中新增对应英文翻译**

en-US 的 `archiveGroup` 从第 155 行开始。在对应位置插入：

```typescript
filters: {
  keyword: "Search",
  keywordPlaceholder: "Group name or code",
  status: "Status",
  statusAll: "All",
  owner: "Owner",
  ownerAll: "All",
  search: "Search",
  reset: "Reset"
},
pager: "Page {page} / {totalPages} · Total {total} records",
emptyFilter: "No matching archive groups found. Try different keywords or reset filters."
```

- [ ] **Step 3: Commit**

```bash
git add easyarchive-ui/src/i18n/messages.ts
git commit -m "feat: 新增归档分组筛选和分页的 i18n 翻译文本"
```

---

### Task 8: 前端 — ArchiveGroupView.vue 核心改造

这是最大的变更任务，分多步完成。

**Files:**
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`

#### 8a: 新增响应式状态

- [ ] **Step 1: 在 `<script setup>` 中新增分页和筛选状态**

在现有 `loading`、`groups` 等 state 之后添加：

```typescript
const pagination = ref({ page: 1, size: 20, total: 0 });
const filters = ref({ keyword: "", enableStatus: undefined as number | undefined, ownerUserId: undefined as number | undefined });
```

#### 8b: 修改 loadData 函数

- [ ] **Step 2: 将 `loadData` 从调用 `getArchiveGroupsApi` 改为调用 `getArchiveGroupsPageApi`**

替换现有 `loadData` 函数：

```typescript
async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const params: ArchiveGroupPageParams = {
      page: pagination.value.page,
      size: pagination.value.size,
      enableStatus: filters.value.enableStatus,
      keyword: filters.value.keyword || undefined,
      ownerUserId: filters.value.ownerUserId,
    };
    const [groupResult, datasourceResult, userResult] = await Promise.all([
      getArchiveGroupsPageApi(params),
      getDatasourcesApi(),
      getUsersApiSilent().catch(() => [] as User[])
    ]);
    groups.value = groupResult.data;
    pagination.value.total = groupResult.total;
    datasources.value = datasourceResult;
    users.value = userResult;
  } finally {
    loading.value = false;
    syncPolling();
  }
}
```

#### 8c: 新增筛选操作函数

- [ ] **Step 3: 新增筛选和重置函数**

```typescript
function handleSearch(): void {
  pagination.value.page = 1;
  void loadData();
}

function handleReset(): void {
  filters.value.keyword = "";
  filters.value.enableStatus = undefined;
  filters.value.ownerUserId = undefined;
  pagination.value.page = 1;
  void loadData();
}

function handlePageChange(page: number): void {
  pagination.value.page = page;
  void loadData();
}

function handleSizeChange(size: number): void {
  pagination.value.size = size;
  pagination.value.page = 1;
  void loadData();
}
```

#### 8d: 新增计算属性

- [ ] **Step 4: 新增分页计算属性**

```typescript
const totalPages = computed(() => Math.ceil(pagination.value.total / pagination.value.size));
const hasActiveFilter = computed(() => !!filters.value.keyword || filters.value.enableStatus !== undefined || !!filters.value.ownerUserId);
```

#### 8e: 修改 import

- [ ] **Step 5: 更新 import 语句**

将：
```typescript
import {
  cancelArchiveGroupActiveTaskApi,
  createArchiveGroupApi,
  deleteArchiveGroupApi,
  type ArchiveGroup,
  type ArchiveGroupPayload,
  getArchiveGroupsApi,
  triggerArchiveGroupApi,
  updateArchiveGroupApi,
  updateArchiveGroupStatusApi
} from "../api/archiveGroup";
```

改为：
```typescript
import {
  cancelArchiveGroupActiveTaskApi,
  createArchiveGroupApi,
  deleteArchiveGroupApi,
  type ArchiveGroup,
  type ArchiveGroupPageParams,
  type ArchiveGroupPayload,
  getArchiveGroupsPageApi,
  triggerArchiveGroupApi,
  updateArchiveGroupApi,
  updateArchiveGroupStatusApi
} from "../api/archiveGroup";
```

同时新增 `computed` 的导入（如果尚未导入）。

#### 8f: 修改 template — 筛选工具栏

- [ ] **Step 6: 在 `page-toolbar` 的 `header` 之后、表格之前新增筛选工具栏**

在 `<div v-if="loading">` 之前插入：

```html
<div class="filter-bar">
  <input
    v-model="filters.keyword"
    type="text"
    :placeholder="t('archiveGroup.filters.keywordPlaceholder')"
    class="filter-bar__input"
  />
  <select v-model.number="filters.enableStatus" class="filter-bar__select" @change="handleSearch">
    <option :value="undefined">{{ t("archiveGroup.filters.statusAll") }}</option>
    <option :value="0">{{ t("status.enabled") }}</option>
    <option :value="1">{{ t("status.disabled") }}</option>
  </select>
  <select v-model.number="filters.ownerUserId" class="filter-bar__select" @change="handleSearch">
    <option :value="undefined">{{ t("archiveGroup.filters.ownerAll") }}</option>
    <option v-for="user in users" :key="user.id" :value="user.id">
      {{ user.realName || user.username }}
    </option>
  </select>
  <button class="btn btn--subtle" @click="handleSearch">{{ t("archiveGroup.filters.search") }}</button>
  <button v-if="hasActiveFilter" class="btn btn--subtle" @click="handleReset">{{ t("archiveGroup.filters.reset") }}</button>
</div>
```

#### 8g: 修改 template — 空状态

- [ ] **Step 7: 修改空状态以区分筛选无结果**

将现有的：
```html
<div v-else-if="!groups.length" class="empty">{{ groupEmptyText }}</div>
```

改为：
```html
<div v-else-if="!groups.length && hasActiveFilter" class="empty">
  {{ t("archiveGroup.emptyFilter") }}
</div>
<div v-else-if="!groups.length" class="empty">{{ groupEmptyText }}</div>
```

#### 8h: 修改 template — 分页栏

- [ ] **Step 8: 在表格 `</table>` 闭合之后、 `</div>` (table-wrap) 之后新增分页栏**

```html
<div v-if="pagination.total > pagination.size" class="pager">
  <div class="pager__info">
    {{ t("archiveGroup.pager", { page: pagination.page, totalPages, total: pagination.total }) }}
  </div>
  <div class="pager__controls">
    <button class="btn btn--subtle" :disabled="pagination.page <= 1 || loading" @click="handlePageChange(pagination.page - 1)">
      {{ t("common.prev") }}
    </button>
    <button class="btn btn--subtle" :disabled="pagination.page >= totalPages || loading" @click="handlePageChange(pagination.page + 1)">
      {{ t("common.next") }}
    </button>
    <select :value="pagination.size" class="pager__size" @change="handleSizeChange(Number(($event.target as HTMLSelectElement).value))">
      <option :value="10">10</option>
      <option :value="20">20</option>
      <option :value="50">50</option>
    </select>
  </div>
</div>
```

- [ ] **Step 9: Commit**

```bash
git add easyarchive-ui/src/views/ArchiveGroupView.vue
git commit -m "feat: ArchiveGroupView 新增筛选工具栏、分页栏和空状态处理"
```

---

### Task 9: 前端 — CSS 样式

**Files:**
- Modify: `easyarchive-ui/src/styles/theme.css`

- [ ] **Step 1: 在 `.row-actions` 样式之后新增筛选栏样式**

```css
/* 筛选工具栏 */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid var(--ea-border);
  border-radius: 12px;
  background: #fafbfc;
}

.filter-bar__input {
  flex: 1 1 200px;
  border: 1px solid var(--ea-border);
  border-radius: 8px;
  padding: 6px 10px;
  min-width: 160px;
}

.filter-bar__select {
  border: 1px solid var(--ea-border);
  border-radius: 8px;
  padding: 6px 10px;
  background: #fff;
}

/* 操作列横向滚动 */
.row-actions {
  overflow-x: auto;
  white-space: nowrap;
  min-width: 200px;
  max-width: 400px;
}

/* 分页栏 */
.pager {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.pager__info {
  color: var(--ea-text-muted);
  font-size: 13px;
}

.pager__controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager__size {
  border: 1px solid var(--ea-border);
  border-radius: 8px;
  padding: 4px 8px;
  background: #fff;
}
```

同时将现有的 `.row-actions` 样式（第 357-361 行）更新为上面的新样式（含 `overflow-x: auto`）。

- [ ] **Step 2: Commit**

```bash
git add easyarchive-ui/src/styles/theme.css
git commit -m "feat: 新增筛选栏样式、分页栏样式和行操作列横向滚动"
```

---

### Task 10: 验证

- [ ] **Step 1: 构建后端**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master && mvn compile -pl easyarchive-starter -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: 运行后端测试**

```bash
mvn test -pl easyarchive-starter -q
```

Expected: 全部测试通过

- [ ] **Step 3: 构建前端**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui && npm run build
```

Expected: 编译成功，无类型错误

- [ ] **Step 4: 手动验证**

启动应用后访问归档分组管理页面，验证：
1. 筛选工具栏显示正常（搜索框 + 状态下拉 + 负责人下拉 + 查询/重置按钮）
2. 分页栏显示正常（页码信息 + 上下页按钮 + 每页条数选择）
3. 操作列在窄屏下可横向滚动，不重叠
4. 筛选条件变更后自动回到第 1 页
5. 数据不足一页时分页栏隐藏
6. 切换每页条数后回到第 1 页

- [ ] **Step 5: 最终 commit（如验证中有修复）**

```bash
git add -u
git commit -m "chore: 验证修复"
```

---

## 注意事项

1. **权限**: `keyword` 和 `ownerUserId` 筛选仅对系统管理员生效（admin 分支），归档管理员和普通用户保持原有权限过滤逻辑
2. **轮询**: 现有轮询机制（`syncPolling`）保留不变，轮询时携带当前筛选参数
3. **i18n**: `common.prev` 和 `common.next` 如果不存在需新增；如已存在则复用
4. **TypeScript**: `URLSearchParams` 在旧浏览器中可能需要 polyfill，但项目目标环境为现代浏览器
