# EasyArchive 界面增强与接口扩展设计

**日期**: 2026-06-26
**状态**: 已确认

## 背景

EasyArchive 数据归档平台有三个核心界面需要增强：归档分组管理、归档任务列表、归档连接管理。当前存在以下问题：
1. 归档分组界面筛选条件在独立行展示，与其他界面风格不统一
2. 归档任务列表缺少归档分组名称列，无法按归档分组筛选
3. 归档连接列表无筛选和分页功能

## 目标

1. 统一所有界面的筛选条件为工具栏布局
2. 增强归档任务列表的筛选和展示能力
3. 为归档连接列表增加筛选和分页功能

## 后端改动

### 1. 任务列表接口增加 groupId 参数

**文件**: `ArchiveTaskLogController.java`
- `GET /api/v1/task-log/tasks` 增加可选参数 `@RequestParam(required = false) Long groupId`
- 透传到 Service 层

**文件**: `ArchiveTaskLogService.java` / 对应 Mapper
- `queryTasks(page, size, status)` → 增加 `groupId` 参数
- SQL: `WHERE group_id = #{groupId}` （当 groupId 不为空时）

### 2. TaskVO 增加 groupName 字段

**文件**: `TaskVO.java`
- 增加字段 `private String groupName;`

**文件**: 任务查询 SQL
- JOIN `archive_group` 表获取 `group_name`，或使用子查询

### 3. 归档连接分页查询接口

**文件**: `DatasourceController.java`
- 新增 `GET /api/v1/archive/datasources/page`
- 参数: `page`(默认1), `size`(默认20), `keyword`(可选), `status`(可选)

**文件**: `DatasourceService.java`
- 新增 `findPage(page, size, keyword, status)` 方法
- 返回 `PageResult<Datasource>` (data + total + page + size)

**文件**: 对应 Mapper
- 新增分页查询 SQL，支持 keyword 模糊匹配和 status 过滤

## 前端改动

### 4. 归档分组界面 (ArchiveGroupView.vue)

**布局调整**:
- 将 `.filter-bar` 中的筛选控件迁移到工具栏右侧区域
- 工具栏结构: `[标题] ——— [关键词输入] [状态下拉] [负责人下拉] [查询] [重置] [刷新] [新建]`
- 删除原有独立的 `.filter-bar` 区域

**功能不变**: 关键词搜索、状态筛选、负责人筛选

### 5. 归档任务列表 (TaskListView.vue)

**表格增加列**:
- 在 ID 列之后增加"归档分组名称"列
- 使用后端返回的 `groupName` 字段直接展示

**工具栏增强**:
- 保留状态筛选下拉
- 增加归档分组下拉选择器（从 API 获取列表，传 `groupId`）
- 增加查询按钮

**API 变更**:
- `getTasksApi()` 增加 `groupId` 参数
- `TaskQuery` 接口增加 `groupId?: number`
- `TaskItem` 接口增加 `groupName?: string`

### 6. 归档连接列表 (DatasourceView.vue)

**工具栏增加**:
- 关键词搜索框（搜索编码/名称）
- 状态下拉筛选（全部/未测试/已启用/已停用）
- 查询按钮 + 重置按钮

**分页控件**:
- 表格底部增加分页组件
- 显示当前页/总页数/总条数
- 上一页/下一页按钮
- 每页条数选择器(10/20/50)

**API 变更**:
- `getDatasourcesApi()` 改为调用分页接口 `/archive/datasources/page`
- 接受参数: `page`, `size`, `keyword`, `status`
- 返回: `{ data: Datasource[], total, page, size }`

## i18n 文案

需要新增以下国际化文案：

| Key (zh-CN) | 中文值 |
|---|---|
| `datasource.filters.keyword` | 搜索 |
| `datasource.filters.keywordPlaceholder` | 连接名称或编码 |
| `datasource.filters.statusAll` | 全部 |
| `datasource.filters.search` | 查询 |
| `datasource.filters.reset` | 重置 |
| `task.filters.group` | 归档分组 |
| `task.filters.groupAll` | 全部 |
| `task.columns.groupName` | 归档分组名称 |

## 变更文件清单

### 后端
- `ArchiveTaskLogController.java` - 增加 groupId 参数
- `ArchiveTaskLogService.java` - 增加 groupId 支持
- 对应 Mapper XML - SQL 增加条件
- `TaskVO.java` - 增加 groupName 字段
- `DatasourceController.java` - 增加分页接口
- `DatasourceService.java` - 增加 findPage 方法
- 对应 Mapper XML - 新增分页 SQL

### 前端
- `src/api/task.ts` - TaskItem 增加 groupName，TaskQuery 增加 groupId
- `src/api/datasource.ts` - getDatasourcesApi 改为分页调用
- `src/views/ArchiveGroupView.vue` - 筛选条件迁移到工具栏
- `src/views/TaskListView.vue` - 增加分组名称列和分组筛选
- `src/views/DatasourceView.vue` - 增加筛选和分页
- `src/i18n/messages.ts` - 新增国际化文案

## 风险与注意事项

1. 归档连接分页接口为新增接口，不影响现有全量列表接口（表单下拉依赖）
2. 任务列表接口增加可选参数，向后兼容
3. TaskVO 增加字段向后兼容（JSON 反序列化忽略未知字段）
4. 前端分页组件可复用现有的分页逻辑模式（已在 ArchiveGroupView 中实现）
