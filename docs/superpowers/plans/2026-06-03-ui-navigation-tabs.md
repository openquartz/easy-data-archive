# UI Navigation & Work Tabs Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add clickable entity links throughout all UI pages and expand the internal work tab system to support both archive group detail and task detail pages.

**Architecture:** Extract the existing local tab state from `AppLayout.vue` into a Pinia store (`workTabs`), generalize the `WorkTab` type to support multiple route types, and create a reusable `EntityLink` component for cross-page navigation. All detail pages open as work tabs; entity references become clickable links.

**Tech Stack:** Vue 3.5, Pinia 2.3, Vue Router 4.5, TypeScript 5.8, Vite 5.4, hand-written CSS (no UI framework)

**Design Spec:** `docs/superpowers/specs/2026-06-03-ui-navigation-tabs-design.md`

---

### Task 1: Add i18n keys for work tabs

**Files:**
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: Add `workTab` keys to both locales**

In `messages.ts`, add a `workTab` section inside both `"en-US"` and `"zh-CN"` objects, after the existing `status` section.

For `"en-US"`, add after the `status` block (after line 326):
```typescript
    workTab: {
      groupDetail: "Group Detail",
      task: "Task"
    }
```

For `"zh-CN"`, add after the `status` block (after line 652):
```typescript
    workTab: {
      groupDetail: "分组详情",
      task: "任务"
    }
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add easyarchive-ui/src/i18n/messages.ts
git commit -m "feat(i18n): add workTab keys for tab title generation"
```

---

### Task 2: Create workTabs Pinia store

**Files:**
- Create: `easyarchive-ui/src/stores/workTabs.ts`

- [ ] **Step 1: Create the store file**

Create `easyarchive-ui/src/stores/workTabs.ts` with the following content:

```typescript
import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { useRouter } from "vue-router";

export interface WorkTabGroupDetail {
  type: "group-detail";
  id: string;
  title: string;
}

export interface WorkTabTaskDetail {
  type: "task-detail";
  taskId: string;
  title: string;
}

export type WorkTab = WorkTabGroupDetail | WorkTabTaskDetail;

export function getTabKey(tab: WorkTab): string {
  if (tab.type === "group-detail") {
    return `group-detail:${tab.id}`;
  }
  return `task-detail:${tab.taskId}`;
}

export const useWorkTabsStore = defineStore("workTabs", () => {
  const router = useRouter();
  const tabs = ref<WorkTab[]>([]);
  const activeKey = ref<string | null>(null);

  const hasTabs = computed(() => tabs.value.length > 0);

  function openTab(tab: WorkTab): void {
    const key = getTabKey(tab);
    activeKey.value = key;
    const existingIndex = tabs.value.findIndex((t) => getTabKey(t) === key);
    if (existingIndex >= 0) {
      tabs.value[existingIndex] = tab;
      return;
    }
    tabs.value = [...tabs.value, tab];
  }

  function closeTab(key: string): void {
    const currentIndex = tabs.value.findIndex((t) => getTabKey(t) === key);
    if (currentIndex < 0) {
      return;
    }
    const closingTab = tabs.value[currentIndex];
    const closingActive = activeKey.value === key;
    const remaining = tabs.value.filter((t) => getTabKey(t) !== key);
    tabs.value = remaining;
    if (!closingActive) {
      return;
    }
    const fallback = remaining[currentIndex] || remaining[currentIndex - 1];
    if (fallback) {
      activeKey.value = getTabKey(fallback);
      void router.push(tabToRoute(fallback));
      return;
    }
    activeKey.value = null;
    const fallbackRoute = closingTab.type === "task-detail"
      ? { name: "tasks" as const }
      : { name: "archive-groups" as const };
    void router.push(fallbackRoute);
  }

  function closeAll(): void {
    tabs.value = [];
    activeKey.value = null;
  }

  function tabToRoute(tab: WorkTab) {
    if (tab.type === "group-detail") {
      return {
        name: "archive-group-detail" as const,
        params: { id: tab.id },
        query: { title: tab.title }
      };
    }
    return {
      name: "task-detail" as const,
      params: { taskId: tab.taskId }
    };
  }

  return {
    tabs,
    activeKey,
    hasTabs,
    openTab,
    closeTab,
    closeAll,
    tabToRoute
  };
});
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add easyarchive-ui/src/stores/workTabs.ts
git commit -m "feat(store): add workTabs Pinia store with generalized tab support"
```

---

### Task 3: Add `.entity-link` CSS styles

**Files:**
- Modify: `easyarchive-ui/src/styles/theme.css`

- [ ] **Step 1: Add entity-link styles**

Append the following to the end of `theme.css` (before the closing `}` of the last `@media` block, or at the very end of the file after line 574):

```css
.entity-link {
  color: var(--ea-primary);
  text-decoration: none;
  cursor: pointer;
  transition: opacity 0.15s;
}

.entity-link:hover {
  text-decoration: underline;
  opacity: 0.85;
}
```

- [ ] **Step 2: Commit**

```bash
git add easyarchive-ui/src/styles/theme.css
git commit -m "feat(style): add .entity-link CSS class for clickable references"
```

---

### Task 4: Create EntityLink component

**Files:**
- Create: `easyarchive-ui/src/components/EntityLink.vue`

- [ ] **Step 1: Create the component**

Create `easyarchive-ui/src/components/EntityLink.vue`:

```vue
<script setup lang="ts">
import { useRouter } from "vue-router";
import { useWorkTabsStore, type WorkTab } from "../stores/workTabs";

const props = defineProps<{
  type: "group" | "task" | "datasource";
  id?: string | number;
  title?: string;
}>();

const router = useRouter();
const workTabsStore = useWorkTabsStore();

function handleClick(event: MouseEvent): void {
  event.preventDefault();
  if (props.type === "group" && props.id != null) {
    const tab: WorkTab = {
      type: "group-detail",
      id: String(props.id),
      title: props.title || ""
    };
    workTabsStore.openTab(tab);
    void router.push(workTabsStore.tabToRoute(tab));
  } else if (props.type === "task" && props.id != null) {
    const tab: WorkTab = {
      type: "task-detail",
      taskId: String(props.id),
      title: `#${props.id}`
    };
    workTabsStore.openTab(tab);
    void router.push(workTabsStore.tabToRoute(tab));
  } else if (props.type === "datasource") {
    void router.push({ name: "datasources" });
  }
}
</script>

<template>
  <a class="entity-link" href="#" @click="handleClick"><slot /></a>
</template>
```

- [ ] **Step 2: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 3: Commit**

```bash
git add easyarchive-ui/src/components/EntityLink.vue
git commit -m "feat(component): add EntityLink for cross-page navigation"
```

---

### Task 5: Refactor AppLayout to use workTabs store

**Files:**
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`

- [ ] **Step 1: Replace local tab state with store**

Replace the entire `<script setup>` block in `AppLayout.vue` with:

```vue
<script setup lang="ts">
import LanguageSwitcher from "../components/LanguageSwitcher.vue";
import { computed, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { useWorkTabsStore, getTabKey } from "../stores/workTabs";

const { t } = useI18n();
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const loggingOut = ref(false);
const workTabsStore = useWorkTabsStore();

const accountLabel = computed(
  () => authStore.profile?.realName || authStore.username || authStore.profile?.username || ""
);

watch(
  () => route.fullPath,
  () => {
    syncWorkTabs();
  },
  { immediate: true }
);

async function handleLogout(): Promise<void> {
  if (loggingOut.value) {
    return;
  }
  loggingOut.value = true;
  try {
    await authStore.logout();
    await router.push({ name: "login" });
  } finally {
    loggingOut.value = false;
  }
}

function resolveParam(value: unknown): string {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : "";
  }
  return value == null ? "" : String(value);
}

function resolveQueryTitle(): string {
  const { title } = route.query;
  if (Array.isArray(title)) {
    return title[0] || t("workTab.groupDetail");
  }
  if (typeof title === "string" && title.trim()) {
    return title;
  }
  return t("workTab.groupDetail");
}

function syncWorkTabs(): void {
  if (route.name === "archive-group-detail") {
    const id = resolveParam(route.params.id);
    if (!id) {
      return;
    }
    workTabsStore.openTab({
      type: "group-detail",
      id,
      title: resolveQueryTitle()
    });
  } else if (route.name === "task-detail") {
    const taskId = resolveParam(route.params.taskId);
    if (!taskId) {
      return;
    }
    workTabsStore.openTab({
      type: "task-detail",
      taskId,
      title: `${t("workTab.task")} #${taskId}`
    });
  }
}

function openWorkTab(tab: { type: string; id?: string; taskId?: string; title?: string }): void {
  if (tab.type === "group-detail" && tab.id) {
    void router.push({
      name: "archive-group-detail",
      params: { id: tab.id },
      query: { title: tab.title }
    });
  } else if (tab.type === "task-detail" && tab.taskId) {
    void router.push({
      name: "task-detail",
      params: { taskId: tab.taskId }
    });
  }
}

function activeWorkTabKey(): string {
  if (!workTabsStore.activeKey) {
    return "";
  }
  return workTabsStore.activeKey;
}
</script>
```

- [ ] **Step 2: Update the template to use store**

Replace the `<template>` block in `AppLayout.vue` with:

```vue
<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <div class="brand">{{ t("layout.brand") }}</div>
      <nav class="nav">
        <RouterLink class="nav__item" :to="{ name: 'dashboard' }">{{ t("layout.nav.dashboard") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'datasources' }">{{ t("layout.nav.datasources") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'archive-groups' }">{{ t("layout.nav.archiveGroups") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'tasks' }">{{ t("layout.nav.tasks") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'guide' }">{{ t("layout.nav.guide") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'users' }">{{ t("layout.nav.users") }}</RouterLink>
      </nav>
    </aside>
    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <strong>{{ t("layout.topbar") }}</strong>
        <div class="app-shell__topbar-actions">
          <span v-if="accountLabel" class="account-pill">{{ accountLabel }}</span>
          <button class="btn btn--subtle" :disabled="loggingOut" @click="handleLogout">
            {{ loggingOut ? t("layout.actions.loggingOut") : t("layout.actions.logout") }}
          </button>
          <LanguageSwitcher />
        </div>
      </header>
      <main class="app-shell__content">
        <div v-if="workTabsStore.hasTabs" class="work-tabs" aria-label="workspace tabs">
          <div
            v-for="tab in workTabsStore.tabs"
            :key="getTabKey(tab)"
            class="work-tabs__item"
            :class="{ 'work-tabs__item--active': getTabKey(tab) === workTabsStore.activeKey }"
          >
            <button type="button" class="work-tabs__trigger" @click="openWorkTab(tab)">{{ tab.title }}</button>
            <button type="button" class="work-tabs__close" aria-label="Close tab" @click="workTabsStore.closeTab(getTabKey(tab))">×</button>
          </div>
        </div>
        <router-view />
      </main>
    </div>
  </div>
</template>
```

- [ ] **Step 3: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add easyarchive-ui/src/layouts/AppLayout.vue
git commit -m "refactor(layout): replace local tab state with workTabs Pinia store"
```

---

### Task 6: Add EntityLink to DashboardView

**Files:**
- Modify: `easyarchive-ui/src/views/DashboardView.vue`

- [ ] **Step 1: Import EntityLink**

In `DashboardView.vue`, add the import after the existing imports (after line 4):
```typescript
import EntityLink from "../components/EntityLink.vue";
```

- [ ] **Step 2: Replace plain text IDs in recent tasks table**

In the recent tasks table `<tbody>` (around line 88-95), replace:
```html
              <tr v-for="item in overview.recentTasks" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.groupId }}</td>
```
with:
```html
              <tr v-for="item in overview.recentTasks" :key="item.id">
                <td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
                <td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
```

- [ ] **Step 3: Replace plain text IDs in failed tasks table**

In the failed tasks table `<tbody>` (around line 118-125), replace:
```html
              <tr v-for="item in overview.failedTasks" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.groupId }}</td>
```
with:
```html
              <tr v-for="item in overview.failedTasks" :key="item.id">
                <td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
                <td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
```

- [ ] **Step 4: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 5: Commit**

```bash
git add easyarchive-ui/src/views/DashboardView.vue
git commit -m "feat(dashboard): add entity links for task and group IDs"
```

---

### Task 7: Add EntityLink to TaskListView

**Files:**
- Modify: `easyarchive-ui/src/views/TaskListView.vue`

- [ ] **Step 1: Import EntityLink**

In `TaskListView.vue`, add the import after the existing imports (after line 5):
```typescript
import EntityLink from "../components/EntityLink.vue";
```

- [ ] **Step 2: Replace plain text IDs in task table**

In the task table `<tbody>` (around line 150-153), replace:
```html
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.groupId }}</td>
```
with:
```html
          <tr v-for="item in list" :key="item.id">
            <td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
            <td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
```

- [ ] **Step 3: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add easyarchive-ui/src/views/TaskListView.vue
git commit -m "feat(tasks): add entity links for task ID and group ID columns"
```

---

### Task 8: Add EntityLink to TaskDetailView

**Files:**
- Modify: `easyarchive-ui/src/views/TaskDetailView.vue`

- [ ] **Step 1: Import EntityLink**

In `TaskDetailView.vue`, add the import after the existing imports (after line 5):
```typescript
import EntityLink from "../components/EntityLink.vue";
```

- [ ] **Step 2: Replace Group ID with EntityLink in detail grid**

In the detail grid (around line 151), replace:
```html
      <p><strong>{{ t("task.columns.groupId") }}:</strong> {{ task.groupId }}</p>
```
with:
```html
      <p><strong>{{ t("task.columns.groupId") }}:</strong> <EntityLink type="group" :id="task.groupId">{{ task.groupId }}</EntityLink></p>
```

- [ ] **Step 3: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add easyarchive-ui/src/views/TaskDetailView.vue
git commit -m "feat(task-detail): add entity link for group ID"
```

---

### Task 9: Add EntityLink to ArchiveGroupDetailView

**Files:**
- Modify: `easyarchive-ui/src/views/ArchiveGroupDetailView.vue`

- [ ] **Step 1: Import EntityLink and datasource API**

In `ArchiveGroupDetailView.vue`, add imports after the existing imports (after line 27):
```typescript
import EntityLink from "../components/EntityLink.vue";
import { getDatasourcesApi, type Datasource } from "../api/datasource";
```

- [ ] **Step 2: Add datasources state and load logic**

In the `<script setup>`, add after the existing state declarations (after line 49, `let loadToken = 0;`):
```typescript
const datasources = ref<Datasource[]>([]);
```

In the `loadDetail()` function, modify the `Promise.all` call (around line 128-131) to also load datasources:
```typescript
    const [itemsResult, overviewResult, datasourceResult] = await Promise.all([
      getArchiveGroupItemsApi(groupId.value),
      getArchiveGroupOverviewApi(groupId.value),
      getDatasourcesApi()
    ]);
```

After the assignment block (after line 139, `overview.value = overviewResult;`), add:
```typescript
    datasources.value = datasourceResult;
```

Add a helper function after the `formatSwitchFlag` function (after line 73):
```typescript
function datasourceName(id: number): string {
  return datasources.value.find((item) => item.id === id)?.datasourceName || String(id);
}
```

- [ ] **Step 3: Replace source/target datasource with EntityLink**

In the detail grid (around lines 360-361), replace:
```html
            <p><strong>{{ t("archiveGroup.columns.source") }}:</strong> {{ group.sourceDatasourceId }}</p>
            <p><strong>{{ t("archiveGroup.columns.target") }}:</strong> {{ group.targetDatasourceId }}</p>
```
with:
```html
            <p><strong>{{ t("archiveGroup.columns.source") }}:</strong> <EntityLink type="datasource" :id="group.sourceDatasourceId">{{ datasourceName(group.sourceDatasourceId) }}</EntityLink></p>
            <p><strong>{{ t("archiveGroup.columns.target") }}:</strong> <EntityLink type="datasource" :id="group.targetDatasourceId">{{ datasourceName(group.targetDatasourceId) }}</EntityLink></p>
```

- [ ] **Step 4: Replace task IDs with EntityLink in recent tasks table**

In the recent tasks table (around line 509), replace:
```html
                  <td>{{ task.id }}</td>
```
with:
```html
                  <td><EntityLink type="task" :id="task.id">{{ task.id }}</EntityLink></td>
```

- [ ] **Step 5: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui/src/views/ArchiveGroupDetailView.vue
git commit -m "feat(group-detail): add entity links for datasources and task IDs"
```

---

### Task 10: Add EntityLink to ArchiveGroupView

**Files:**
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`

- [ ] **Step 1: Import EntityLink**

In `ArchiveGroupView.vue`, add the import after the existing component imports (after line 14):
```typescript
import EntityLink from "../components/EntityLink.vue";
```

- [ ] **Step 2: Add datasourceCode helper**

In the `<script setup>`, add a helper after the `datasourceName` function (after line 44):
```typescript
const datasourceCode = (id: number): string => datasources.value.find((item) => item.id === id)?.datasourceCode || "";
```

- [ ] **Step 3: Replace group code with EntityLink**

In the table body (around line 222), replace:
```html
            <td>{{ group.groupCode }}</td>
```
with:
```html
            <td><EntityLink type="group" :id="group.id" :title="group.groupName">{{ group.groupCode }}</EntityLink></td>
```

- [ ] **Step 4: Replace source/target with EntityLink**

In the table body (around lines 223-224), replace:
```html
            <td>{{ datasourceName(group.sourceDatasourceId) }}</td>
            <td>{{ datasourceName(group.targetDatasourceId) }}</td>
```
with:
```html
            <td><EntityLink type="datasource" :id="group.sourceDatasourceId">{{ datasourceName(group.sourceDatasourceId) }}</EntityLink></td>
            <td><EntityLink type="datasource" :id="group.targetDatasourceId">{{ datasourceName(group.targetDatasourceId) }}</EntityLink></td>
```

- [ ] **Step 5: Verify TypeScript compiles**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui/src/views/ArchiveGroupView.vue
git commit -m "feat(groups): add entity links for group code and datasource names"
```

---

### Task 11: Smoke test — build and verify

**Files:**
- None (verification only)

- [ ] **Step 1: Run TypeScript type check**

Run: `cd easyarchive-ui && npx tsc --noEmit`
Expected: No errors

- [ ] **Step 2: Run Vite build**

Run: `cd easyarchive-ui && npm run build`
Expected: Build succeeds with no errors

- [ ] **Step 3: Run smoke check script**

Run: `cd easyarchive-ui && node scripts/smoke-check.mjs`
Expected: Smoke check passes

- [ ] **Step 4: Final commit if any fixes were needed**

```bash
git add -A
git commit -m "fix: address build issues from navigation feature"
```
(Skip this step if no fixes were needed)
