<script setup lang="ts">
import { computed, ref } from "vue";
import { getDashboardOverviewApi, type DashboardOverview } from "../api/dashboard";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import { useI18n } from "../i18n";

const loading = ref(false);
const errorMessage = ref("");
const overview = ref<DashboardOverview | null>(null);
const { t } = useI18n();

const statusCountMap = computed(() => {
  const result = new Map<number, number>();
  for (const item of overview.value?.taskStatusCounts || []) {
    const status = item.executeStatus ?? item.status;
    if (status === undefined || status === null) {
      continue;
    }
    result.set(Number(status), Number(item.count) || 0);
  }
  return result;
});

const summaryCards = computed(() => {
  const datasource = overview.value?.datasourceStatusSummary;
  return [
    { label: t("dashboard.cards.running"), value: statusCountMap.value.get(1) || 0 },
    { label: t("dashboard.cards.succeeded"), value: statusCountMap.value.get(2) || 0 },
    { label: t("dashboard.cards.failed"), value: statusCountMap.value.get(3) || 0 },
    { label: t("dashboard.cards.datasourceEnabled"), value: datasource?.enabled || 0 },
    { label: t("dashboard.cards.datasourceDisabled"), value: datasource?.disabled || 0 },
    { label: t("dashboard.cards.datasourceTotal"), value: datasource?.total || 0 }
  ];
});

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    overview.value = await getDashboardOverviewApi();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("dashboard.loadFailed");
  } finally {
    loading.value = false;
  }
}

void loadData();
</script>

<template>
  <section class="dashboard-page">
    <header class="page-toolbar">
      <h1>{{ t("dashboard.title") }}</h1>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
      </div>
    </header>

    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-if="loading" class="page-card empty">{{ t("dashboard.loading") }}</div>
    <div v-else-if="!overview" class="page-card empty">{{ t("dashboard.empty") }}</div>
    <template v-else>
      <section class="dashboard-cards">
        <article v-for="item in summaryCards" :key="item.label" class="metric-card">
          <p class="metric-card__label">{{ item.label }}</p>
          <p class="metric-card__value">{{ item.value }}</p>
        </article>
      </section>

      <section class="page-card">
        <h2 class="section-title section-title--top">{{ t("dashboard.recentTasks") }}</h2>
        <div v-if="!overview.recentTasks.length" class="empty">{{ t("dashboard.noRecentTasks") }}</div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t("dashboard.columns.id") }}</th>
                <th>{{ t("dashboard.columns.groupId") }}</th>
                <th>{{ t("dashboard.columns.status") }}</th>
                <th>{{ t("dashboard.columns.processed") }}</th>
                <th>{{ t("dashboard.columns.speed") }}</th>
                <th>{{ t("dashboard.columns.startTime") }}</th>
                <th>{{ t("dashboard.columns.endTime") }}</th>
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
        <h2 class="section-title section-title--top">{{ t("dashboard.failedTasks") }}</h2>
        <div v-if="!overview.failedTasks.length" class="empty">{{ t("dashboard.noFailedTasks") }}</div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t("dashboard.columns.id") }}</th>
                <th>{{ t("dashboard.columns.groupId") }}</th>
                <th>{{ t("dashboard.columns.status") }}</th>
                <th>{{ t("dashboard.columns.error") }}</th>
                <th>{{ t("dashboard.columns.startTime") }}</th>
                <th>{{ t("dashboard.columns.endTime") }}</th>
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
