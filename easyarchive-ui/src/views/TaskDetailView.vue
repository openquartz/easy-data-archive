<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { cancelTaskApi, getTaskDetailApi, getTaskLogsApi, type TaskItem, type TaskLogItem } from "../api/task";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import { useI18n } from "../i18n";
import EntityLink from "../components/EntityLink.vue";
import { createPolling } from "../utils/polling";
import { formatTaskLogConsoleMeta, getTaskLogConsoleLevelTone } from "../utils/taskLogConsole";

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
const cancelButtonText = computed(() =>
  task.value?.executeStatus === 4 ? t("task.cancelling") : t("task.cancelAction")
);
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
  if (!canCancel.value || cancelling.value || !task.value || task.value.executeStatus === 4) {
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

function resolveLogTone(level?: string): string {
  return `task-log-console__level--${getTaskLogConsoleLevelTone(level)}`;
}

function resolveLogMeta(item: TaskLogItem): string {
  return formatTaskLogConsoleMeta(item.logType, item.executePhase);
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
        <button class="btn btn--primary" :disabled="!canCancel || cancelling || task?.executeStatus === 4" @click="cancelTask">
          {{ cancelling ? t("task.cancelling") : cancelButtonText }}
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
      <p><strong>{{ t("task.columns.groupId") }}:</strong> <EntityLink type="group" :id="task.groupId">{{ task.groupId }}</EntityLink></p>
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
    <div v-else class="task-log-console">
      <div v-for="item in logs" :key="item.id" class="task-log-console__row">
        <div class="task-log-console__main">
          <span class="task-log-console__time">{{ item.logTime || "-" }}</span>
          <span class="task-log-console__level" :class="resolveLogTone(item.logLevel)">
            {{ item.logLevel || "-" }}
          </span>
          <span class="task-log-console__meta">{{ resolveLogMeta(item) }}</span>
        </div>
        <div class="task-log-console__content">{{ item.logContent || "-" }}</div>
        <div class="task-log-console__stats">
          <span>{{ t("task.columns.processed") }} {{ item.processedCount ?? "-" }}</span>
          <span>{{ t("task.columns.speed") }} {{ item.processSpeed ?? "-" }}</span>
        </div>
      </div>
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

<style scoped>
.task-log-console {
  display: grid;
  gap: 10px;
  padding: 16px;
  border: 1px solid #1f2937;
  border-radius: 14px;
  background:
    radial-gradient(circle at top left, rgba(34, 197, 94, 0.08), transparent 28%),
    linear-gradient(180deg, #05080f 0%, #0a0f18 100%);
  box-shadow: inset 0 1px 0 rgba(148, 163, 184, 0.08), inset 0 0 0 1px rgba(15, 23, 42, 0.45);
}

.task-log-console__row {
  display: grid;
  gap: 8px;
  padding: 12px 14px;
  border: 1px solid rgba(71, 85, 105, 0.45);
  border-radius: 10px;
  background: rgba(15, 23, 42, 0.72);
  font-family: "IBM Plex Mono", "SFMono-Regular", "Consolas", monospace;
}

.task-log-console__main,
.task-log-console__stats {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.task-log-console__time,
.task-log-console__meta,
.task-log-console__stats {
  color: #94a3b8;
  font-size: 13px;
}

.task-log-console__level {
  display: inline-flex;
  align-items: center;
  min-width: 58px;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid currentColor;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.task-log-console__level--success {
  color: #22c55e;
  background: rgba(34, 197, 94, 0.12);
}

.task-log-console__level--warning {
  color: #facc15;
  background: rgba(250, 204, 21, 0.14);
}

.task-log-console__level--danger {
  color: #f87171;
  background: rgba(248, 113, 113, 0.14);
}

.task-log-console__level--neutral {
  color: #cbd5e1;
  background: rgba(148, 163, 184, 0.12);
}

.task-log-console__content {
  color: #e2e8f0;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

@media (max-width: 720px) {
  .task-log-console {
    padding: 12px;
  }

  .task-log-console__row {
    padding: 10px 12px;
  }
}
</style>
