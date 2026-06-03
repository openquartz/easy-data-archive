<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRouter } from "vue-router";
import { cancelTaskApi, getTasksApi, type TaskItem } from "../api/task";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import EntityLink from "../components/EntityLink.vue";
import { useI18n } from "../i18n";
import { createPolling } from "../utils/polling";

const router = useRouter();
const loading = ref(false);
const list = ref<TaskItem[]>([]);
const errorMessage = ref("");
const page = ref(1);
const size = ref(20);
const total = ref(0);
const statusFilter = ref("");
const cancellingIds = ref(new Set<number>());
const { t } = useI18n();

const emptyText = computed(() => (loading.value ? t("task.emptyLoading") : t("task.empty")));
const statusOptions = computed(() => [
  { label: t("task.filters.all"), value: "" },
  { label: t("task.status.waiting"), value: "0" },
  { label: t("task.status.running"), value: "1" },
  { label: t("task.status.success"), value: "2" },
  { label: t("task.status.failed"), value: "3" },
  { label: t("task.status.cancelling"), value: "4" },
  { label: t("task.status.cancelled"), value: "5" }
]);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));
const pagerText = computed(() =>
  t("task.pager", { page: page.value, totalPages: totalPages.value, total: total.value })
);

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const result = await getTasksApi({ page: page.value, size: size.value, status: statusFilter.value || undefined });
    list.value = result.list || [];
    total.value = result.total || 0;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("task.loadFailed");
  } finally {
    loading.value = false;
  }
}

function goDetail(taskId: number): void {
  void router.push({ name: "task-detail", params: { taskId } });
}

function canCancel(task: TaskItem): boolean {
  return task.executeStatus === 0 || task.executeStatus === 1 || task.executeStatus === 4;
}

async function cancelTask(task: TaskItem): Promise<void> {
  if (!canCancel(task) || cancellingIds.value.has(task.id)) {
    return;
  }
  const confirmed = window.confirm(t("task.cancelConfirm", { id: task.id }));
  if (!confirmed) {
    return;
  }
  cancellingIds.value.add(task.id);
  errorMessage.value = "";
  try {
    await cancelTaskApi(task.id);
    await loadData();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("task.cancelFailed");
  } finally {
    cancellingIds.value.delete(task.id);
  }
}

function applyFilter(): void {
  page.value = 1;
  void loadData();
}

function refresh(): void {
  void loadData();
}

function prevPage(): void {
  if (page.value <= 1 || loading.value) {
    return;
  }
  page.value -= 1;
  void loadData();
}

function nextPage(): void {
  if (page.value >= totalPages.value || loading.value) {
    return;
  }
  page.value += 1;
  void loadData();
}

const poller = createPolling(loadData, { intervalMs: 5000, immediate: false });

onMounted(async () => {
  await loadData();
  poller.start();
});

onBeforeUnmount(() => {
  poller.stop();
});

onBeforeRouteLeave(() => {
  poller.stop();
});
</script>

<template>
  <section class="page-card">
    <header class="page-toolbar">
      <h1>{{ t("task.title") }}</h1>
      <div class="actions">
        <select v-model="statusFilter" :disabled="loading" @change="applyFilter">
          <option v-for="item in statusOptions" :key="item.value || 'all'" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <button class="btn btn--subtle" :disabled="loading" @click="refresh">{{ t("common.refresh") }}</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-if="loading" class="empty">{{ emptyText }}</div>
    <div v-else-if="!list.length" class="empty">{{ emptyText }}</div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>{{ t("task.columns.id") }}</th>
            <th>{{ t("task.columns.groupId") }}</th>
            <th>{{ t("task.columns.status") }}</th>
            <th>{{ t("task.columns.processed") }}</th>
            <th>{{ t("task.columns.speed") }}</th>
            <th>{{ t("task.columns.startTime") }}</th>
            <th>{{ t("task.columns.endTime") }}</th>
            <th>{{ t("task.columns.actions") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
            <td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
            <td><TaskStatusTag :status="item.executeStatus" /></td>
            <td>{{ item.processedRecords ?? 0 }}</td>
            <td>{{ item.processedSpeed ?? "-" }}</td>
            <td>{{ item.startTime || "-" }}</td>
            <td>{{ item.endTime || "-" }}</td>
            <td>
              <button
                v-if="canCancel(item)"
                class="btn btn--subtle"
                :disabled="cancellingIds.has(item.id) || item.executeStatus === 4"
                @click="cancelTask(item)"
              >
                {{ item.executeStatus === 4 ? t("task.cancelling") : t("task.cancelAction") }}
              </button>
              <button class="btn btn--subtle" @click="goDetail(item.id)">{{ t("common.detail") }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <footer class="pager">
      <span>{{ pagerText }}</span>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="page <= 1 || loading" @click="prevPage">{{ t("common.prev") }}</button>
        <button class="btn btn--subtle" :disabled="page >= totalPages || loading" @click="nextPage">{{ t("common.next") }}</button>
      </div>
    </footer>
  </section>
</template>
