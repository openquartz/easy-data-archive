<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { cancelTaskApi, getTaskDetailApi, getTaskLogsApi, type TaskItem, type TaskLogItem } from "../api/task";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import { useI18n } from "../i18n";
import { createPolling } from "../utils/polling";

const route = useRoute();
const router = useRouter();
const taskId = computed(() => Number(route.params.taskId));
const loading = ref(false);
const loadingLogs = ref(false);
const cancelling = ref(false);
const errorMessage = ref("");
const actionErrorMessage = ref("");
const successMessage = ref("");
const task = ref<TaskItem | null>(null);
const logs = ref<TaskLogItem[]>([]);
const logPage = ref(1);
const logSize = ref(20);
const logTotal = ref(0);
const { t } = useI18n();

const canCancel = computed(() => {
  const status = task.value?.executeStatus;
  return status === 0 || status === 1 || status === 4;
});
const logTotalPages = computed(() => Math.max(1, Math.ceil(logTotal.value / logSize.value)));
const emptyLogText = computed(() => (loadingLogs.value ? t("task.logEmptyLoading") : t("task.logEmpty")));
const logPagerText = computed(() =>
  t("task.pager", { page: logPage.value, totalPages: logTotalPages.value, total: logTotal.value })
);

async function loadTask(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    task.value = await getTaskDetailApi(taskId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("task.detailLoadFailed");
  } finally {
    loading.value = false;
  }
}

async function loadLogs(): Promise<void> {
  loadingLogs.value = true;
  errorMessage.value = "";
  try {
    const result = await getTaskLogsApi(taskId.value, { page: logPage.value, size: logSize.value });
    logs.value = result.list || [];
    logTotal.value = result.total || 0;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("task.logsLoadFailed");
  } finally {
    loadingLogs.value = false;
  }
}

async function loadAll(): Promise<void> {
  await Promise.all([loadTask(), loadLogs()]);
}

async function cancelTask(): Promise<void> {
  if (!canCancel.value || cancelling.value || !task.value) {
    return;
  }
  const confirmed = window.confirm(t("task.cancelConfirm", { id: task.value.id }));
  if (!confirmed) {
    return;
  }
  cancelling.value = true;
  actionErrorMessage.value = "";
  successMessage.value = "";
  try {
    await cancelTaskApi(task.value.id);
    successMessage.value = t("task.cancelSubmitted");
    await loadAll();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("task.cancelFailed");
  } finally {
    cancelling.value = false;
  }
}

function refresh(): void {
  void loadAll();
}

function prevLogPage(): void {
  if (logPage.value <= 1 || loadingLogs.value) {
    return;
  }
  logPage.value -= 1;
  void loadLogs();
}

function nextLogPage(): void {
  if (logPage.value >= logTotalPages.value || loadingLogs.value) {
    return;
  }
  logPage.value += 1;
  void loadLogs();
}

const poller = createPolling(loadAll, { intervalMs: 5000, immediate: false });
const stopPolling = (): void => poller.stop();

onMounted(async () => {
  if (!Number.isFinite(taskId.value)) {
    errorMessage.value = t("task.invalidId");
    return;
  }
  await loadAll();
  poller.start();
});

onBeforeRouteLeave(() => {
  stopPolling();
});

onBeforeUnmount(() => {
  stopPolling();
});
</script>

<template>
  <section class="page-card">
    <header class="page-toolbar">
      <h1>{{ t("task.detailTitle") }}</h1>
      <div class="actions">
        <button class="btn btn--subtle" @click="router.push({ name: 'tasks' })">{{ t("common.back") }}</button>
        <button class="btn btn--subtle" :disabled="loading || loadingLogs" @click="refresh">{{ t("common.refresh") }}</button>
        <button class="btn btn--primary" :disabled="!canCancel || cancelling" @click="cancelTask">
          {{ cancelling ? t("task.cancelling") : t("task.cancelAction") }}
        </button>
      </div>
    </header>
    <p v-if="successMessage" class="feedback">{{ successMessage }}</p>
    <p v-if="actionErrorMessage" class="error">{{ actionErrorMessage }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div v-if="loading" class="empty">{{ t("task.detailLoading") }}</div>
    <div v-else-if="!task" class="empty">{{ t("task.detailEmpty") }}</div>
    <div v-else class="detail-grid">
      <p><strong>{{ t("task.columns.id") }}:</strong> {{ task.id }}</p>
      <p><strong>{{ t("task.columns.groupId") }}:</strong> {{ task.groupId }}</p>
      <p><strong>{{ t("task.columns.status") }}:</strong> <TaskStatusTag :status="task.executeStatus" /></p>
      <p><strong>{{ t("task.columns.processed") }}:</strong> {{ task.processedRecords ?? 0 }}</p>
      <p><strong>{{ t("task.columns.speed") }}:</strong> {{ task.processedSpeed ?? "-" }}</p>
      <p><strong>{{ t("task.columns.heartbeat") }}:</strong> {{ task.heartbeatTime || "-" }}</p>
      <p><strong>{{ t("task.columns.start") }}:</strong> {{ task.startTime || "-" }}</p>
      <p><strong>{{ t("task.columns.end") }}:</strong> {{ task.endTime || "-" }}</p>
      <p class="full-width"><strong>{{ t("task.columns.error") }}:</strong> {{ task.errorMsg || "-" }}</p>
    </div>

    <h2 class="section-title">{{ t("task.logs") }}</h2>
    <div v-if="loadingLogs" class="empty">{{ emptyLogText }}</div>
    <div v-else-if="!logs.length" class="empty">{{ emptyLogText }}</div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>{{ t("task.columns.time") }}</th>
            <th>{{ t("task.columns.level") }}</th>
            <th>{{ t("task.columns.type") }}</th>
            <th>{{ t("task.columns.phase") }}</th>
            <th>{{ t("task.columns.processed") }}</th>
            <th>{{ t("task.columns.speed") }}</th>
            <th>{{ t("task.columns.content") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in logs" :key="item.id">
            <td>{{ item.logTime || "-" }}</td>
            <td>{{ item.logLevel || "-" }}</td>
            <td>{{ item.logType || "-" }}</td>
            <td>{{ item.executePhase || "-" }}</td>
            <td>{{ item.processedCount ?? "-" }}</td>
            <td>{{ item.processSpeed ?? "-" }}</td>
            <td>{{ item.logContent || "-" }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    <footer class="pager">
      <span>{{ logPagerText }}</span>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="logPage <= 1 || loadingLogs" @click="prevLogPage">{{ t("common.prev") }}</button>
        <button class="btn btn--subtle" :disabled="logPage >= logTotalPages || loadingLogs" @click="nextLogPage">{{ t("common.next") }}</button>
      </div>
    </footer>
  </section>
</template>
