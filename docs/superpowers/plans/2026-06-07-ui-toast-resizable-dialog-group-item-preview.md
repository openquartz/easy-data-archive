# UI Toast、可拉伸弹窗与归档分组明细预览 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified toast feedback layer for backend request results, make targeted create/edit dialogs resizable, and add a read-only archive-group item preview dialog directly from the archive group list.

**Architecture:** Add a lightweight toast store plus `ToastContainer` mounted at the app root, then migrate target management views from inline backend success/error banners to toast calls. Keep the existing custom modal system and extend it with shared resizable dialog styles, then add one focused preview dialog component that reads existing archive-group item summary data and renders separate TIME and ID sections.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Axios, Node test runner

---

## File Map

- Modify: `easyarchive-ui/src/App.vue`
  - Mount the global toast container once at app root.
- Create: `easyarchive-ui/src/stores/toast.ts`
  - Hold toast state, auto-dismiss timers, and `showSuccessToast` / `showErrorToast`.
- Create: `easyarchive-ui/src/components/ToastContainer.vue`
  - Render stacked toast cards and optional manual dismiss button.
- Modify: `easyarchive-ui/src/utils/http.ts`
  - Trigger global error toast for backend and network failures without breaking auth-expired behavior.
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
  - Replace inline backend success/error banners with toasts and add preview-dialog entrypoint state.
- Create: `easyarchive-ui/src/components/ArchiveGroupItemsPreviewDialog.vue`
  - Render read-only TIME / ID item sections from `ArchiveGroupItemSummary[]`.
- Modify: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
  - Apply shared resizable dialog class and content scrolling.
- Modify: `easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue`
  - Apply shared resizable dialog class and better textarea sizing.
- Modify: `easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue`
  - Apply shared resizable dialog class and better textarea sizing.
- Modify: `easyarchive-ui/src/components/DatasourceFormDialog.vue`
  - Apply shared resizable dialog class.
- Modify: `easyarchive-ui/src/components/UserFormDialog.vue`
  - Apply shared resizable dialog class.
- Modify: `easyarchive-ui/src/components/UserPermissionDialog.vue`
  - Apply shared resizable dialog class while preserving its wider layout.
- Modify: `easyarchive-ui/src/i18n/messages.ts`
  - Add labels for preview dialog and toast dismiss text if needed.
- Create: `easyarchive-ui/tests/toast-store.test.ts`
  - Cover toast queue and auto-dismiss behavior.
- Create: `easyarchive-ui/tests/archive-group-items-preview.test.ts`
  - Cover TIME / ID grouping and empty states for preview dialog helpers.
- Modify: `easyarchive-ui/tests/archive-group-form.test.ts`
  - Extend tests if any helper extraction for dialog classes or textarea defaults needs coverage.

## Task 1: Add Toast Store Coverage First

**Files:**
- Create: `easyarchive-ui/tests/toast-store.test.ts`
- Test target: `easyarchive-ui/src/stores/toast.ts`

- [ ] **Step 1: Write the failing toast store test**

```ts
import test from "node:test";
import assert from "node:assert/strict";

import {
  clearToasts,
  getToastSnapshot,
  showErrorToast,
  showSuccessToast
} from "../src/stores/toast";

test("toast store appends toasts and allows clearing", () => {
  clearToasts();

  const successId = showSuccessToast("Saved", 5000);
  const errorId = showErrorToast("Request failed", 5000);
  const snapshot = getToastSnapshot();

  assert.equal(snapshot.length, 2);
  assert.equal(snapshot[0]?.id, successId);
  assert.equal(snapshot[0]?.type, "success");
  assert.equal(snapshot[1]?.id, errorId);
  assert.equal(snapshot[1]?.type, "error");

  clearToasts();
  assert.deepEqual(getToastSnapshot(), []);
});

test("toast store auto dismisses after the configured duration", async () => {
  clearToasts();

  showErrorToast("Transient error", 10);
  assert.equal(getToastSnapshot().length, 1);

  await new Promise((resolve) => setTimeout(resolve, 30));

  assert.deepEqual(getToastSnapshot(), []);
});
```

- [ ] **Step 2: Run the toast store test to verify it fails**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test -- --test-name-pattern="toast store"
```

Expected: FAIL because `../src/stores/toast` does not exist yet.

- [ ] **Step 3: Write the minimal toast store implementation**

```ts
import { readonly, ref } from "vue";

export type ToastType = "success" | "error";

export interface ToastItem {
  id: number;
  type: ToastType;
  message: string;
  durationMs: number;
}

const toasts = ref<ToastItem[]>([]);
const timers = new Map<number, ReturnType<typeof setTimeout>>();
let seed = 0;

function removeToast(id: number): void {
  const timer = timers.get(id);
  if (timer) {
    clearTimeout(timer);
    timers.delete(id);
  }
  toasts.value = toasts.value.filter((item) => item.id !== id);
}

function pushToast(type: ToastType, message: string, durationMs = 5000): number {
  const id = ++seed;
  toasts.value = [...toasts.value, { id, type, message, durationMs }];
  timers.set(id, setTimeout(() => removeToast(id), durationMs));
  return id;
}

export function showSuccessToast(message: string, durationMs = 5000): number {
  return pushToast("success", message, durationMs);
}

export function showErrorToast(message: string, durationMs = 5000): number {
  return pushToast("error", message, durationMs);
}

export function clearToasts(): void {
  for (const id of timers.keys()) {
    removeToast(id);
  }
  toasts.value = [];
}

export function getToastSnapshot(): ToastItem[] {
  return [...toasts.value];
}

export const toastStore = readonly(toasts);
export { removeToast };
```

- [ ] **Step 4: Run the toast store test to verify it passes**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test -- --test-name-pattern="toast store"
```

Expected: PASS with 2 passing toast-store tests.

- [ ] **Step 5: Commit the toast store foundation**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui/src/stores/toast.ts easyarchive-ui/tests/toast-store.test.ts
git commit -m "feat: add ui toast store"
```

## Task 2: Mount the Global Toast UI and Wire HTTP Error Toasts

**Files:**
- Create: `easyarchive-ui/src/components/ToastContainer.vue`
- Modify: `easyarchive-ui/src/App.vue`
- Modify: `easyarchive-ui/src/utils/http.ts`
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: Add a focused container component test seam by keeping toast rendering simple**

Create the component with direct store rendering and no extra abstraction:

```vue
<script setup lang="ts">
import { toastStore, removeToast } from "../stores/toast";
</script>

<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="true">
    <article
      v-for="item in toastStore"
      :key="item.id"
      class="toast"
      :class="[`toast--${item.type}`]"
      role="status"
    >
      <p>{{ item.message }}</p>
      <button type="button" class="toast__close" @click="removeToast(item.id)">×</button>
    </article>
  </div>
</template>
```

- [ ] **Step 2: Mount the toast container at the root**

Update `easyarchive-ui/src/App.vue` to:

```vue
<script setup lang="ts">
import ToastContainer from "./components/ToastContainer.vue";
</script>

<template>
  <router-view />
  <ToastContainer />
</template>
```

- [ ] **Step 3: Trigger error toast from the HTTP layer**

Update the response interceptor in `easyarchive-ui/src/utils/http.ts`:

```ts
import { showErrorToast } from "../stores/toast";

function emitHttpErrorToast(message: string): void {
  if (message.trim()) {
    showErrorToast(message, 5000);
  }
}

client.interceptors.response.use(
  <T>(response: { data: ApiResponse<T> | T | null; status: number }) => {
    const payload = response.data as ApiResponse<T> | T | null;
    if (payload && typeof payload === "object" && "code" in payload) {
      const envelope = payload as ApiResponse<T>;
      if (envelope.code !== API_SUCCESS_CODE) {
        const message = envelope.message || "Request failed";
        emitHttpErrorToast(message);
        throw createApiError(message, {
          status: response.status,
          code: envelope.code,
          requestId: envelope.requestId
        });
      }
      return envelope.data;
    }
    return payload as T;
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    const status = error.response?.status;
    const payload = error.response?.data;

    if (status === 401) {
      clearAuthAndSignal();
      emitHttpErrorToast(payload?.message || "Unauthorized");
      throw createApiError("Unauthorized", { status: 401 });
    }

    const message = payload?.message || error.message || "Request failed";
    emitHttpErrorToast(message);
    throw createApiError(message, {
      status,
      code: payload?.code,
      requestId: payload?.requestId
    });
  }
);
```

- [ ] **Step 4: Add any new i18n keys used by the container**

Update `easyarchive-ui/src/i18n/messages.ts` with keys such as:

```ts
common: {
  // existing keys...
  dismiss: "Dismiss"
}
```

And set the close button label:

```vue
<button type="button" class="toast__close" :aria-label="t('common.dismiss')" @click="removeToast(item.id)">×</button>
```

- [ ] **Step 5: Run the full frontend test suite and build**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test
npm run build
```

Expected: PASS for tests and a successful Vite production build.

- [ ] **Step 6: Commit root toast integration**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui/src/App.vue easyarchive-ui/src/components/ToastContainer.vue easyarchive-ui/src/utils/http.ts easyarchive-ui/src/i18n/messages.ts
git commit -m "feat: add global backend error toasts"
```

## Task 3: Migrate Archive Group View to Toasts and Add Success Notifications

**Files:**
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Modify: `easyarchive-ui/src/stores/toast.ts` (if success helper names need refinement)

- [ ] **Step 1: Replace inline backend feedback state with toast calls**

Refactor `easyarchive-ui/src/views/ArchiveGroupView.vue` by removing:

```ts
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
```

And introducing:

```ts
import { showSuccessToast } from "../stores/toast";
```

Use toast calls in success paths:

```ts
showSuccessToast(t("archiveGroup.created"));
showSuccessToast(t("archiveGroup.updated"));
showSuccessToast(t("archiveGroup.statusUpdated"));
showSuccessToast(t("archiveGroup.deleted"));
showSuccessToast(t("archiveGroup.triggered").replace("{id}", String(task.id)));
showSuccessToast(t("archiveGroup.cancelSubmitted").replace("{id}", String(task.id)));
```

- [ ] **Step 2: Stop rendering old inline success/error banners**

Delete this block from the template:

```vue
<p v-if="successMessage" class="feedback">{{ successMessage }}</p>
<p v-if="actionErrorMessage" class="error">{{ actionErrorMessage }}</p>
<p v-if="errorMessage" class="error">{{ errorMessage }}</p>
```

And let backend failures bubble to `http.ts` toast behavior.

- [ ] **Step 3: Preserve non-backend loading behavior without swallowing errors**

Adjust `loadData` and `runGroupAction` to rethrow only when the caller needs control, but avoid storing backend errors in local refs:

```ts
async function loadData(): Promise<void> {
  loading.value = true;
  try {
    const [groupResult, datasourceResult, userResult] = await Promise.all([
      getArchiveGroupsApi(),
      getDatasourcesApi(),
      getUsersApi()
    ]);
    groups.value = groupResult;
    datasources.value = datasourceResult;
    users.value = userResult;
  } finally {
    loading.value = false;
    syncPolling();
  }
}
```

- [ ] **Step 4: Run focused archive-group tests and full test suite**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test -- --test-name-pattern="archive group|notification"
npm test
```

Expected: PASS without relying on old inline banner assertions.

- [ ] **Step 5: Commit archive-group toast migration**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui/src/views/ArchiveGroupView.vue
git commit -m "feat: move archive group feedback to toasts"
```

## Task 4: Add Shared Resizable Dialog Styling for Targeted Form Dialogs

**Files:**
- Modify: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- Modify: `easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue`
- Modify: `easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue`
- Modify: `easyarchive-ui/src/components/DatasourceFormDialog.vue`
- Modify: `easyarchive-ui/src/components/UserFormDialog.vue`
- Modify: `easyarchive-ui/src/components/UserPermissionDialog.vue`

- [ ] **Step 1: Add a shared modal class contract to each target dialog**

Update each `<section class="modal-card">` to include a shared resizable modifier:

```vue
<section class="modal-card modal-card--resizable">
```

For user permissions keep the wide modifier too:

```vue
<section class="modal-card modal-card--wide modal-card--resizable">
```

- [ ] **Step 2: Add scoped styles that make dialogs resizable and scroll-safe**

Append the same scoped style block to each form dialog component that does not already define modal styles:

```vue
<style scoped>
.modal-card--resizable {
  width: min(880px, calc(100vw - 3rem));
  min-width: 560px;
  min-height: 360px;
  max-width: 96vw;
  max-height: 90vh;
  overflow: auto;
  resize: both;
}

.modal-card--resizable .form-grid {
  align-content: start;
}

.modal-card--resizable textarea {
  min-height: 8rem;
  resize: vertical;
}
</style>
```

For `UserPermissionDialog.vue`, merge this into the existing `<style scoped>` block instead of creating a second style block.

- [ ] **Step 3: Preserve usability on narrow screens**

Adjust responsive constraints so mobile still works:

```css
@media (max-width: 640px) {
  .modal-card--resizable {
    min-width: 0;
    width: calc(100vw - 1.5rem);
    max-height: 92vh;
    resize: none;
  }
}
```

- [ ] **Step 4: Run build and smoke tests for dialog markup**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test
npm run build
```

Expected: PASS with no template/style compilation errors.

- [ ] **Step 5: Commit resizable dialog styling**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui/src/components/ArchiveGroupFormDialog.vue easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue easyarchive-ui/src/components/DatasourceFormDialog.vue easyarchive-ui/src/components/UserFormDialog.vue easyarchive-ui/src/components/UserPermissionDialog.vue
git commit -m "feat: add resizable management dialogs"
```

## Task 5: Add Archive Group Items Preview Coverage First

**Files:**
- Create: `easyarchive-ui/tests/archive-group-items-preview.test.ts`
- Create target helper inside: `easyarchive-ui/src/components/ArchiveGroupItemsPreviewDialog.vue`

- [ ] **Step 1: Write a focused grouping helper test**

```ts
import test from "node:test";
import assert from "node:assert/strict";

import { splitArchiveGroupItemsByType } from "../src/components/ArchiveGroupItemsPreviewDialog.vue";

test("splitArchiveGroupItemsByType groups summary items into time and id buckets", () => {
  const result = splitArchiveGroupItemsByType([
    { id: 1, itemType: "TIME", sourceTable: "orders", targetTable: "orders_hist", groupId: 1, priority: 10 },
    { id: 2, itemType: "ID", sourceTable: "pay", targetTable: "pay_hist", groupId: 1, priority: 20 }
  ]);

  assert.equal(result.timeItems.length, 1);
  assert.equal(result.idItems.length, 1);
  assert.equal(result.timeItems[0]?.id, 1);
  assert.equal(result.idItems[0]?.id, 2);
});

test("splitArchiveGroupItemsByType keeps empty buckets for empty input", () => {
  const result = splitArchiveGroupItemsByType([]);

  assert.deepEqual(result.timeItems, []);
  assert.deepEqual(result.idItems, []);
});
```

- [ ] **Step 2: Run the preview helper test to verify it fails**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test -- --test-name-pattern="splitArchiveGroupItemsByType"
```

Expected: FAIL because the preview dialog and helper do not exist yet.

- [ ] **Step 3: Create the preview dialog with an exported pure helper**

Implement the helper at the top of `easyarchive-ui/src/components/ArchiveGroupItemsPreviewDialog.vue`:

```vue
<script lang="ts">
import type { ArchiveGroupItemSummary } from "../api/archiveGroupItem";

export function splitArchiveGroupItemsByType(items: ArchiveGroupItemSummary[]) {
  return {
    timeItems: items.filter((item) => item.itemType === "TIME"),
    idItems: items.filter((item) => item.itemType === "ID")
  };
}
</script>
```

Then add the `<script setup>` and template to render:

```vue
<script setup lang="ts">
import type { ArchiveGroupItemSummary } from "../api/archiveGroupItem";
import { computed } from "vue";
import { useI18n } from "../i18n";
import { formatArchiveGroupItemRange } from "../utils/archiveGroupItemRange";
import { getStatusLabel, archiveEnableStatusDictionary } from "../utils/dictionaries";

const props = defineProps<{
  visible: boolean;
  loading?: boolean;
  groupName?: string;
  items: ArchiveGroupItemSummary[];
}>();

const emit = defineEmits<{ (event: "close"): void }>();
const { t } = useI18n();
const grouped = computed(() => splitArchiveGroupItemsByType(props.items));
</script>
```

- [ ] **Step 4: Re-run the preview helper test**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test -- --test-name-pattern="splitArchiveGroupItemsByType"
```

Expected: PASS with 2 passing preview helper tests.

- [ ] **Step 5: Commit the preview dialog foundation**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui/src/components/ArchiveGroupItemsPreviewDialog.vue easyarchive-ui/tests/archive-group-items-preview.test.ts
git commit -m "feat: add archive group items preview dialog"
```

## Task 6: Wire Archive Group Preview Dialog into the List View

**Files:**
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] **Step 1: Add preview dialog state and loader logic**

Extend `easyarchive-ui/src/views/ArchiveGroupView.vue` with:

```ts
import { getArchiveGroupItemsApi, type ArchiveGroupItemSummary } from "../api/archiveGroupItem";
import ArchiveGroupItemsPreviewDialog from "../components/ArchiveGroupItemsPreviewDialog.vue";

const previewDialogVisible = ref(false);
const previewDialogLoading = ref(false);
const previewDialogItems = ref<ArchiveGroupItemSummary[]>([]);
const previewDialogGroupName = ref("");
```

And add the opener:

```ts
async function openItemsPreview(group: ArchiveGroup): Promise<void> {
  if (isRowBusy(group.id)) {
    return;
  }
  await runGroupAction("preview", group.id, async () => {
    previewDialogLoading.value = true;
    previewDialogItems.value = [];
    previewDialogGroupName.value = group.groupName;
    previewDialogVisible.value = true;
    previewDialogItems.value = await getArchiveGroupItemsApi(group.id);
  }).finally(() => {
    previewDialogLoading.value = false;
  });
}

function closeItemsPreview(): void {
  previewDialogVisible.value = false;
  previewDialogLoading.value = false;
  previewDialogItems.value = [];
  previewDialogGroupName.value = "";
}
```

- [ ] **Step 2: Add the new action button beside Edit**

Update the actions column to insert:

```vue
<button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="openItemsPreview(group)">
  {{ t("archiveGroup.actions.previewItems") }}
</button>
<button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="openEditGroup(group)">
  {{ t("common.edit") }}
</button>
```

- [ ] **Step 3: Render the dialog at the bottom of the view**

```vue
<ArchiveGroupItemsPreviewDialog
  :visible="previewDialogVisible"
  :loading="previewDialogLoading"
  :group-name="previewDialogGroupName"
  :items="previewDialogItems"
  @close="closeItemsPreview"
/>
```

- [ ] **Step 4: Add missing i18n keys**

Add keys like:

```ts
archiveGroup: {
  actions: {
    previewItems: "Details"
  },
  preview: {
    title: "Archive Group Items - {name}",
    timeSection: "Time Items",
    idSection: "ID Items",
    emptyTime: "No time-based items",
    emptyId: "No ID-based items"
  }
}
```

Also add matching Chinese translations in the Chinese locale block.

- [ ] **Step 5: Run full frontend verification**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test
npm run build
```

Expected: PASS and no TypeScript errors for the new preview dialog integration.

- [ ] **Step 6: Commit archive-group preview wiring**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui/src/views/ArchiveGroupView.vue easyarchive-ui/src/i18n/messages.ts
git commit -m "feat: add archive group item preview action"
```

## Task 7: Final Verification and Manual UI Check

**Files:**
- No new code files; verify the implementation as integrated.

- [ ] **Step 1: Run the complete frontend test suite**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm test
```

Expected: PASS for all Node-based frontend tests.

- [ ] **Step 2: Run a production build**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm run build
```

Expected: PASS with generated production assets.

- [ ] **Step 3: Run the smoke script**

Run:

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui
npm run smoke
```

Expected: PASS with build + smoke validation completed.

- [ ] **Step 4: Manually verify the three user-facing requirements**

Check:

```text
1. Trigger a backend error from archive groups, datasource, user, or item operations and confirm an error toast appears for 5 seconds.
2. Open each targeted create/edit dialog and confirm it can be resized larger while retaining usable scrolling.
3. Click the new Details button next to Edit in the archive-group list and confirm the dialog shows separate TIME and ID read-only lists.
```

- [ ] **Step 5: Commit any final fixes from verification**

```bash
cd /Users/jackxu/Documents/Code/local/easy-archive-master
git add easyarchive-ui
git commit -m "test: verify ui toast and dialog interactions"
```

## Self-Review

- Spec coverage:
  - Backend error toast: covered by Tasks 1-3 and 7.
  - Resizable target dialogs: covered by Task 4 and 7.
  - Archive-group list preview dialog: covered by Tasks 5-6 and 7.
- Placeholder scan:
  - No `TODO`, `TBD`, or “similar to previous task” placeholders remain.
- Type consistency:
  - `showSuccessToast`, `showErrorToast`, `splitArchiveGroupItemsByType`, `previewDialogItems`, and `ArchiveGroupItemSummary` names are used consistently across tasks.
