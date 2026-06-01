<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from "vue";
import { onBeforeRouteLeave, useRouter } from "vue-router";
import { getTasksApi, type TaskItem } from "../api/task";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import { createPolling } from "../utils/polling";

const router = useRouter();
const loading = ref(false);
const list = ref<TaskItem[]>([]);
const errorMessage = ref("");
const page = ref(1);
const size = ref(20);
const total = ref(0);
const statusFilter = ref("");

const emptyText = computed(() => (loading.value ? "Loading tasks..." : "No task records."));
const statusOptions = [
  { label: "All", value: "" },
  { label: "Waiting", value: "0" },
  { label: "Running", value: "1" },
  { label: "Success", value: "2" },
  { label: "Failed", value: "3" },
  { label: "Cancelling", value: "4" },
  { label: "Cancelled", value: "5" }
];
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const result = await getTasksApi({ page: page.value, size: size.value, status: statusFilter.value || undefined });
    list.value = result.list || [];
    total.value = result.total || 0;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Failed to load tasks";
  } finally {
    loading.value = false;
  }
}

function goDetail(taskId: number): void {
  void router.push({ name: "task-detail", params: { taskId } });
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
      <h1>Archive Tasks</h1>
      <div class="actions">
        <select v-model="statusFilter" :disabled="loading" @change="applyFilter">
          <option v-for="item in statusOptions" :key="item.value || 'all'" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <button class="btn btn--subtle" :disabled="loading" @click="refresh">Refresh</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-if="loading" class="empty">{{ emptyText }}</div>
    <div v-else-if="!list.length" class="empty">{{ emptyText }}</div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Group ID</th>
            <th>Status</th>
            <th>Processed</th>
            <th>Speed</th>
            <th>Start Time</th>
            <th>End Time</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.id }}</td>
            <td>{{ item.groupId }}</td>
            <td><TaskStatusTag :status="item.executeStatus" /></td>
            <td>{{ item.processedRecords ?? 0 }}</td>
            <td>{{ item.processedSpeed ?? "-" }}</td>
            <td>{{ item.startTime || "-" }}</td>
            <td>{{ item.endTime || "-" }}</td>
            <td>
              <button class="btn btn--subtle" @click="goDetail(item.id)">Detail</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <footer class="pager">
      <span>Page {{ page }} / {{ totalPages }} · Total {{ total }}</span>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="page <= 1 || loading" @click="prevPage">Prev</button>
        <button class="btn btn--subtle" :disabled="page >= totalPages || loading" @click="nextPage">Next</button>
      </div>
    </footer>
  </section>
</template>
