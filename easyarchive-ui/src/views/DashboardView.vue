<script setup lang="ts">
import { computed, ref } from "vue";
import { getDashboardOverviewApi, type DashboardOverview } from "../api/dashboard";
import TaskStatusTag from "../components/TaskStatusTag.vue";

const loading = ref(false);
const errorMessage = ref("");
const overview = ref<DashboardOverview | null>(null);

const statusCountMap = computed(() => {
  const result = new Map<number, number>();
  for (const item of overview.value?.taskStatusCounts || []) {
    result.set(Number(item.executeStatus), Number(item.count) || 0);
  }
  return result;
});

const summaryCards = computed(() => {
  const datasource = overview.value?.datasourceStatusSummary;
  return [
    { label: "Tasks Running", value: statusCountMap.value.get(1) || 0 },
    { label: "Tasks Succeeded", value: statusCountMap.value.get(2) || 0 },
    { label: "Tasks Failed", value: statusCountMap.value.get(3) || 0 },
    { label: "Datasources Enabled", value: datasource?.enabled || 0 },
    { label: "Datasources Disabled", value: datasource?.disabled || 0 },
    { label: "Datasources Total", value: datasource?.total || 0 }
  ];
});

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    overview.value = await getDashboardOverviewApi();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Failed to load dashboard";
  } finally {
    loading.value = false;
  }
}

void loadData();
</script>

<template>
  <section class="dashboard-page">
    <header class="page-toolbar">
      <h1>Dashboard</h1>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">Refresh</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-if="loading" class="page-card empty">Loading dashboard...</div>
    <div v-else-if="!overview" class="page-card empty">No dashboard data.</div>
    <template v-else>
      <section class="dashboard-cards">
        <article v-for="item in summaryCards" :key="item.label" class="metric-card">
          <p class="metric-card__label">{{ item.label }}</p>
          <p class="metric-card__value">{{ item.value }}</p>
        </article>
      </section>

      <section class="page-card">
        <h2 class="section-title section-title--top">Recent Tasks</h2>
        <div v-if="!overview.recentTasks.length" class="empty">No recent tasks.</div>
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
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in overview.recentTasks" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.groupId }}</td>
                <td><TaskStatusTag :status="item.executeStatus" /></td>
                <td>{{ item.processedRecords ?? 0 }}</td>
                <td>{{ item.processedSpeed ?? "-" }}</td>
                <td>{{ item.startTime || "-" }}</td>
                <td>{{ item.endTime || "-" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="page-card">
        <h2 class="section-title section-title--top">Failed Tasks</h2>
        <div v-if="!overview.failedTasks.length" class="empty">No failed tasks.</div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Group ID</th>
                <th>Status</th>
                <th>Error</th>
                <th>Start Time</th>
                <th>End Time</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in overview.failedTasks" :key="item.id">
                <td>{{ item.id }}</td>
                <td>{{ item.groupId }}</td>
                <td><TaskStatusTag :status="item.executeStatus" /></td>
                <td class="truncate" :title="item.errorMsg || '-'">{{ item.errorMsg || "-" }}</td>
                <td>{{ item.startTime || "-" }}</td>
                <td>{{ item.endTime || "-" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </section>
</template>
