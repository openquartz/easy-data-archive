# 归档分组管理界面 — 分页、筛选与按钮布局优化

**日期**: 2026-06-25  
**状态**: 已确认  
**范围**: `easyarchive-ui` 前端 + `easyarchive-starter` 后端

---

## 背景

归档分组管理界面（`ArchiveGroupView.vue`）当前存在以下问题：
1. 所有分组数据一次性加载，数据量大时性能差
2. 无法按名称/编码或负责人筛选
3. 操作列按钮过多，屏幕缩小时可能重叠

## 方案概述

- **后端**：扩展已有的 `/api/v1/archive/groups/page` 接口，新增 `keyword` 和 `ownerUserId` 参数
- **前端**：新增筛选工具栏、分页组件，操作列改为横向滚动
- **风格**：复用现有原生表格风格，不引入第三方 UI 库

## 详细设计

### 1. 后端 API 变更

**接口**: `GET /api/v1/archive/groups/page`

新增参数：

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| `keyword` | String | 否 | - | 分组名称/编码模糊搜索 |
| `ownerUserId` | Long | 否 | - | 负责人 ID |
| `enableStatus` | Integer | 否 | - | 0=启用, 1=停用, 不传=全部 |
| `page` | Integer | 否 | 1 | 页码 |
| `size` | Integer | 否 | 20 | 每页条数 |

**返回类型**（不变）: `PageResult<ArchiveGroupView>`

```json
{
  "data": [...],
  "total": 100,
  "page": 1,
  "size": 20
}
```

**修改文件**:
- `ArchiveGroupController.java` — `/page` 端点增加 `@RequestParam` 接收
- `ArchiveGroupService.java` — `findPage` 方法签名增加 `keyword`、`ownerUserId`
- `ArchiveGroupMapper.xml` — SQL 增加动态 `LIKE` 和 `owner_user_id` 条件

### 2. 前端 API 层

**文件**: `easyarchive-ui/src/api/archiveGroup.ts`

修改现有函数，支持新参数：

```typescript
export interface ArchiveGroupPageParams {
  page?: number;
  size?: number;
  enableStatus?: number;
  keyword?: string;
  ownerUserId?: number;
}

export function getArchiveGroupsPageApi(params: ArchiveGroupPageParams): Promise<PageResult<ArchiveGroup>> {
  const query = new URLSearchParams();
  if (params.page) query.set("page", String(params.page));
  if (params.size) query.set("size", String(params.size));
  if (params.enableStatus !== undefined) query.set("enableStatus", String(params.enableStatus));
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.ownerUserId) query.set("ownerUserId", String(params.ownerUserId));
  return http.get<PageResult<ArchiveGroup>>(`/archive/groups/page?${query.toString()}`);
}
```

### 3. 前端 UI 变更

#### 3.1 筛选工具栏

在 `page-toolbar` 区域增加筛选表单（位于标题下方、表格上方）：

```
[搜索输入框] [状态下拉 ▼] [负责人下拉 ▼] [查询] [重置]
```

- 搜索输入框：输入分组名称或编码，点击「查询」触发
- 状态下拉：全部 / 启用 / 停用（选择后自动触发）
- 负责人下拉：从 `users` 列表填充（选择后自动触发）
- 重置：清空所有筛选条件，回到第 1 页

#### 3.2 分页栏

在表格下方增加：

```
[上一页] 第 1 / 5 页 · 共 100 条 [下一页]    [每页 ▼ 10/20/50]
```

- 数据不足一页时隐藏分页栏
- 切换每页条数时重置到第 1 页
- 加载期间禁用分页按钮

#### 3.3 操作列防重叠

对 `.row-actions` 增加样式：

```css
.row-actions {
  overflow-x: auto;
  white-space: nowrap;
  min-width: 200px;
  max-width: 400px;
}
```

按钮始终单行排列，超出列宽时出现横向滚动条。

### 4. i18n 新增文本

**文件**: `easyarchive-ui/src/i18n/messages.ts`

新增中文翻译：

```typescript
archiveGroup: {
  // 现有... 
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
  pageSize: "每页 {size} 条",
  emptyFilter: "未找到匹配条件的归档分组，请尝试其他关键词或重置筛选"
}
```

### 5. 异常处理

| 场景 | 处理 |
|------|------|
| 加载中 | 表格区域显示加载提示，分页/筛选按钮禁用 |
| 请求失败 | toast 提示错误，保留上次数据 |
| 筛选无结果 | 显示空状态提示 |
| 当前页超出范围 | 自动回到最后一页 |
| 快速切换分页 | 使用请求 token 防止旧请求覆盖新结果 |

### 6. 修改文件清单

| 文件 | 变更 |
|------|------|
| `easyarchive-ui/src/api/archiveGroup.ts` | 新增分页查询函数和参数接口 |
| `easyarchive-ui/src/views/ArchiveGroupView.vue` | 新增筛选工具栏、分页栏、操作列滚动 |
| `easyarchive-ui/src/i18n/messages.ts` | 新增 i18n 翻译文本 |
| `easyarchive-ui/src/styles/theme.css` | 新增操作列滚动样式 |
| `easyarchive-starter/.../ArchiveGroupController.java` | `/page` 端点增加参数 |
| `easyarchive-starter/.../ArchiveGroupService.java` | `findPage` 方法增加筛选参数 |
| `easyarchive-starter/.../ArchiveGroupMapper.xml` | SQL 增加动态条件 |
