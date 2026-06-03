# UI Navigation & Work Tabs Design Spec

## Overview

Add cross-page entity links and expand the internal work tab system to support all detail pages (group detail + task detail). Currently only `archive-group-detail` routes create work tabs; this design generalizes the tab system and adds clickable entity references throughout the UI.

## Goals

1. All detail pages (archive group detail, task detail) open as work tabs
2. Entity references (group ID, task ID, datasource name) are clickable links throughout the UI
3. Consistent styling and interaction patterns across all pages
4. Tab state managed via Pinia store for reusability

## Architecture

### WorkTab Type System

```typescript
// stores/workTabs.ts

interface WorkTabGroupDetail {
  type: 'group-detail'
  id: string
  title: string
}

interface WorkTabTaskDetail {
  type: 'task-detail'
  taskId: string
  title: string
}

type WorkTab = WorkTabGroupDetail | WorkTabTaskDetail
```

### Pinia Store: `workTabs`

**File:** `src/stores/workTabs.ts`

**State:**
- `tabs: WorkTab[]` — currently open work tabs
- `activeKey: string | null` — key of the currently active tab

**Getters:**
- `hasTabs: boolean` — whether any tabs exist

**Actions:**
- `openTab(tab: WorkTab): void` — open a new tab or activate an existing one (dedup by key)
- `closeTab(key: string): void` — close a tab; if it was active, switch to adjacent tab or fallback route
- `closeAll(): void` — close all tabs

**Helper:**
- `getTabKey(tab: WorkTab): string` — generates unique key:
  - `group-detail:{id}` for group tabs
  - `task-detail:{taskId}` for task tabs

**Behavior rules:**
- Opening an already-existing tab only activates it (no duplicate)
- Closing the active tab auto-switches to the nearest remaining tab
- When the last tab is closed, navigate back to the parent list page (`archive-groups` for group tabs, `tasks` for task tabs)
- Closing a non-active tab only removes it without navigation

### AppLayout Refactoring

**File:** `src/layouts/AppLayout.vue`

**Remove:** local `workTabs` ref, `syncWorkTabs()`, `openWorkTab()`, `closeWorkTab()`, `activeWorkTabKey` computed

**Add:** import `useWorkTabsStore()`

**Route watcher** (`watch(route)`):
- When `route.name === 'archive-group-detail'`:
  - `store.openTab({ type: 'group-detail', id: route.params.id, title: route.query.title || t('workTab.groupDetail') })`
- When `route.name === 'task-detail'`:
  - `store.openTab({ type: 'task-detail', taskId: route.params.taskId, title: t('workTab.taskDetail') + ' #' + route.params.taskId })`
- Other routes: no tab creation

**Tab rendering:** unchanged pill-style tabs with close buttons, using store state instead of local ref

**Tab click → navigation:** `openWorkTab(tab)` calls `router.push()` based on `tab.type`:
- `group-detail` → `{ name: 'archive-group-detail', params: { id: tab.id }, query: { title: tab.title } }`
- `task-detail` → `{ name: 'task-detail', params: { taskId: tab.taskId } }`

### EntityLink Component

**File:** `src/components/EntityLink.vue`

**Props:**

| Prop | Type | Required | Description |
|------|------|----------|-------------|
| `type` | `'group' \| 'task' \| 'datasource'` | yes | Entity type |
| `id` | `string \| number` | conditional | Entity ID (group/task) |
| `title` | `string` | no | Group title (passed to tab) |
| `code` | `string` | conditional | Datasource code |

**Behavior:**
- `type="group"` → `workTabsStore.openTab({ type: 'group-detail', id, title })` → creates tab + navigates
- `type="task"` → `workTabsStore.openTab({ type: 'task-detail', taskId: id })` → creates tab + navigates
- `type="datasource"` → `router.push({ name: 'datasources' })` (list page, no tab)

**Rendering:** `<a>` tag with CSS class `.entity-link`

**Styling:**
- Color: `var(--color-primary)` (#4f6ef7)
- Hover: underline + slight opacity change
- Consistent with existing link patterns in the codebase

### i18n Additions

**File:** `src/i18n/messages.ts`

New keys:
```
workTab.groupDetail    zh: "分组详情"    en: "Group Detail"
workTab.taskDetail     zh: "任务"        en: "Task"
```

Tab title format:
- Group: uses `route.query.title` (existing behavior)
- Task: `{t('workTab.taskDetail')} #${taskId}` → `任务 #101` / `Task #101`

## Page-by-Page Link Changes

### DashboardView (`/dashboard`)

| Location | Current | New |
|----------|---------|-----|
| Recent tasks table — ID column | Plain text `task.id` | `<EntityLink type="task" :id="task.id" />` |
| Recent tasks table — Group ID column | Plain text `task.groupId` | `<EntityLink type="group" :id="task.groupId" />` |
| Failed tasks table — ID column | Plain text | `<EntityLink type="task" :id="task.id" />` |
| Failed tasks table — Group ID column | Plain text | `<EntityLink type="group" :id="task.groupId" />` |

### TaskListView (`/tasks`)

| Location | Current | New |
|----------|---------|-----|
| Task table — ID column | Plain text | `<EntityLink type="task" :id="task.id" />` |
| Task table — Group ID column | Plain text `task.groupId` | `<EntityLink type="group" :id="task.groupId" />` |

### TaskDetailView (`/tasks/:taskId`)

| Location | Current | New |
|----------|---------|-----|
| Detail grid — Group ID field | Plain text | `<EntityLink type="group" :id="task.groupId" />` |

### ArchiveGroupDetailView (`/archive/groups/:id/detail`)

| Location | Current | New |
|----------|---------|-----|
| Metadata — Source datasource | Plain text name | `<EntityLink type="datasource" :code="group.sourceCode" />` |
| Metadata — Target datasource | Plain text name | `<EntityLink type="datasource" :code="group.targetCode" />` |
| Recent tasks table — ID column | Plain text | `<EntityLink type="task" :id="task.id" />` |

### ArchiveGroupView (`/archive/groups`)

| Location | Current | New |
|----------|---------|-----|
| Group table — Code column | Plain text | `<EntityLink type="group" :id="group.id" :title="group.name" />` |
| Source column | Plain text name | `<EntityLink type="datasource" :code="group.sourceCode" />` |
| Target column | Plain text name | `<EntityLink type="datasource" :code="group.targetCode" />` |

### DatasourceView (`/datasources`)

No changes. Datasource is a leaf entity with no downstream detail page.

### UserView (`/users`)

No changes. User management has no associated detail pages.

## Files to Create

| File | Description |
|------|-------------|
| `src/stores/workTabs.ts` | Pinia store for work tab state |
| `src/components/EntityLink.vue` | Reusable entity link component |

## Files to Modify

| File | Changes |
|------|---------|
| `src/layouts/AppLayout.vue` | Replace local tab state with store, generalize syncWorkTabs |
| `src/views/DashboardView.vue` | Replace plain text IDs with EntityLink |
| `src/views/TaskListView.vue` | Replace plain text IDs with EntityLink |
| `src/views/TaskDetailView.vue` | Replace Group ID with EntityLink |
| `src/views/ArchiveGroupDetailView.vue` | Add EntityLink for datasource names and task IDs |
| `src/views/ArchiveGroupView.vue` | Add EntityLink for group code and datasource names |
| `src/i18n/messages.ts` | Add workTab.* i18n keys |
| `src/styles/theme.css` | Add `.entity-link` styles |

## Styling Details

### `.entity-link` CSS

```css
.entity-link {
  color: var(--color-primary);
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.15s;
}

.entity-link:hover {
  text-decoration: underline;
  opacity: 0.85;
}
```

### Tab bar

No CSS changes needed — existing `.work-tabs` styles are reused as-is.

## Back Button Behavior

The "Back" button on detail pages (ArchiveGroupDetailView, TaskDetailView) retains its existing behavior: a simple `router.push()` to the parent list page. It does NOT close the work tab. This keeps the tab available for later reference while allowing the user to navigate back to the list.

## Scope Exclusions

- Tab state is NOT persisted to localStorage (matching current behavior)
- No breadcrumb navigation (out of scope)
- Metric cards on Dashboard are NOT made clickable (low value, would need filtering logic)
- DatasourceView and UserView receive no links (leaf entities)
