<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRoute, useRouter } from "vue-router";
import { cancelTaskApi, getTaskDetailApi, getTaskLogsApi, type TaskItem, type TaskLogItem } from "../api/task";
import TaskStatusTag from "../components/TaskStatusTag.vue";
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

const canCancel = computed(() => {
  const status = task.value?.executeStatus;
  return status === 0 || status === 1 || status === 4;
});
const logTotalPages = computed(() => Math.max(1, Math.ceil(logTotal.value / logSize.value)));
const emptyLogText = computed(() => (loadingLogs.value ? "Loading logs..." : "No log records."));

async function loadTask(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    task.value = await getTaskDetailApi(taskId.value);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Failed to load task detail";
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
    errorMessage.value = error instanceof Error ? error.message : "Failed to load task logs";
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
  const confirmed = window.confirm(`Cancel task #${task.value.id}?`);
  if (!confirmed) {
    return;
  }
  cancelling.value = true;
  actionErrorMessage.value = "";
  successMessage.value = "";
  try {
    await cancelTaskApi(task.value.id);
    successMessage.value = "Cancel request submitted.";
    await loadAll();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : "Cancel failed";
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
    errorMessage.value = "Invalid task ID.";
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
      <h1>Task Detail</h1>
      <div class="actions">
        <button class="btn btn--subtle" @click="router.push({ name: 'tasks' })">Back</button>
        <button class="btn btn--subtle" :disabled="loading || loadingLogs" @click="refresh">Refresh</button>
        <button class="btn btn--primary" :disabled="!canCancel || cancelling" @click="cancelTask">
          {{ cancelling ? "Cancelling..." : "Cancel Task" }}
        </button>
      </div>
    </header>
    <p v-if="successMessage" class="feedback">{{ successMessage }}</p>
    <p v-if="actionErrorMessage" class="error">{{ actionErrorMessage }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div v-if="loading" class="empty">Loading task detail...</div>
    <div v-else-if="!task" class="empty">Task not found.</div>
    <div v-else class="detail-grid">
      <p><strong>ID:</strong> {{ task.id }}</p>
      <p><strong>Group ID:</strong> {{ task.groupId }}</p>
      <p><strong>Status:</strong> <TaskStatusTag :status="task.executeStatus" /></p>
      <p><strong>Processed:</strong> {{ task.processedRecords ?? 0 }}</p>
      <p><strong>Speed:</strong> {{ task.processedSpeed ?? "-" }}</p>
      <p><strong>Heartbeat:</strong> {{ task.heartbeatTime || "-" }}</p>
      <p><strong>Start:</strong> {{ task.startTime || "-" }}</p>
      <p><strong>End:</strong> {{ task.endTime || "-" }}</p>
      <p class="full-width"><strong>Error:</strong> {{ task.errorMsg || "-" }}</p>
    </div>

    <h2 class="section-title">Logs</h2>
    <div v-if="loadingLogs" class="empty">{{ emptyLogText }}</div>
    <div v-else-if="!logs.length" class="empty">{{ emptyLogText }}</div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>Time</th>
            <th>Level</th>
            <th>Type</th>
            <th>Phase</th>
            <th>Processed</th>
            <th>Speed</th>
            <th>Content</th>
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
      <span>Page {{ logPage }} / {{ logTotalPages }} · Total {{ logTotal }}</span>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="logPage <= 1 || loadingLogs" @click="prevLogPage">Prev</button>
        <button class="btn btn--subtle" :disabled="logPage >= logTotalPages || loadingLogs" @click="nextLogPage">Next</button>
      </div>
    </footer>
  </section>
</template>
