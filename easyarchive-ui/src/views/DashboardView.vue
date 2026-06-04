<script setup lang="ts">
import { computed, ref } from "vue";
import { getDashboardOverviewApi, type DashboardOverview } from "../api/dashboard";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import { useI18n } from "../i18n";
import EntityLink from "../components/EntityLink.vue";

const loading = ref(false);
const errorMessage = ref("");
const overview = ref<DashboardOverview | null>(null);
const { t } = useI18n();
const chartWidth = 640;
const chartHeight = 240;
const chartPadding = { top: 24, right: 20, bottom: 32, left: 36 };

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

const trendItems = computed(() => overview.value?.dailyTaskTrend || []);

const trendMaxValue = computed(() =>
  Math.max(
    1,
    ...trendItems.value.flatMap((item) => [item.submittedCount, item.successCount, item.failedCount])
  )
);

const trendSeries = computed(() => {
  const width = chartWidth - chartPadding.left - chartPadding.right;
  const height = chartHeight - chartPadding.top - chartPadding.bottom;
  const stepX = trendItems.value.length > 1 ? width / (trendItems.value.length - 1) : 0;
  const scaleY = (value: number) => chartPadding.top + height - (value / trendMaxValue.value) * height;
  const buildPoints = (key: "submittedCount" | "successCount" | "failedCount") =>
    trendItems.value.map((item, index) => ({
      x: chartPadding.left + stepX * index,
      y: scaleY(item[key]),
      value: item[key],
      day: item.day
    }));

  return [
    { key: "submitted", label: t("dashboard.trend.submitted"), color: "#2563eb", points: buildPoints("submittedCount") },
    { key: "success", label: t("dashboard.trend.success"), color: "#16a34a", points: buildPoints("successCount") },
    { key: "failed", label: t("dashboard.trend.failed"), color: "#dc2626", points: buildPoints("failedCount") }
  ];
});

const trendYAxisTicks = computed(() => {
  const tickCount = Math.min(4, trendMaxValue.value);
  return Array.from({ length: tickCount + 1 }, (_, index) => {
    const value = Math.round((trendMaxValue.value * (tickCount - index)) / tickCount);
    const usableHeight = chartHeight - chartPadding.top - chartPadding.bottom;
    const y = chartPadding.top + (usableHeight * index) / tickCount;
    return { value, y };
  });
});

function pointsToPolyline(points: Array<{ x: number; y: number }>): string {
  return points.map((point) => `${point.x},${point.y}`).join(" ");
}

function shortDayLabel(day: string): string {
  return day.slice(5);
}

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
        <div class="dashboard-section-header">
          <h2 class="section-title section-title--top">{{ t("dashboard.trend.title") }}</h2>
          <p class="dashboard-section-note">{{ t("dashboard.trend.subtitle") }}</p>
        </div>
        <div v-if="!trendItems.length" class="empty">{{ t("dashboard.trend.empty") }}</div>
        <div v-else class="dashboard-trend">
          <div class="dashboard-trend__legend">
            <span v-for="series in trendSeries" :key="series.key" class="dashboard-trend__legend-item">
              <span class="dashboard-trend__legend-dot" :style="{ backgroundColor: series.color }"></span>
              {{ series.label }}
            </span>
          </div>
          <svg class="dashboard-trend__chart" :viewBox="`0 0 ${chartWidth} ${chartHeight}`" role="img" aria-label="task trend chart">
            <g v-for="tick in trendYAxisTicks" :key="tick.y">
              <line
                :x1="chartPadding.left"
                :x2="chartWidth - chartPadding.right"
                :y1="tick.y"
                :y2="tick.y"
                class="dashboard-trend__grid"
              />
              <text :x="chartPadding.left - 8" :y="tick.y + 4" class="dashboard-trend__axis-label dashboard-trend__axis-label--left">
                {{ tick.value }}
              </text>
            </g>
            <polyline
              v-for="series in trendSeries"
              :key="series.key"
              :points="pointsToPolyline(series.points)"
              :stroke="series.color"
              class="dashboard-trend__line"
            />
            <g v-for="series in trendSeries" :key="`${series.key}-points`">
              <circle
                v-for="point in series.points"
                :key="`${series.key}-${point.day}`"
                :cx="point.x"
                :cy="point.y"
                r="3.5"
                :fill="series.color"
              >
                <title>{{ `${series.label} ${point.day}: ${point.value}` }}</title>
              </circle>
            </g>
            <text
              v-for="(item, index) in trendItems"
              :key="item.day"
              :x="chartPadding.left + ((chartWidth - chartPadding.left - chartPadding.right) * index) / Math.max(trendItems.length - 1, 1)"
              :y="chartHeight - 10"
              class="dashboard-trend__axis-label"
              text-anchor="middle"
            >
              {{ shortDayLabel(item.day) }}
            </text>
          </svg>
        </div>
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
                <td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
                <td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
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
                <td><EntityLink type="task" :id="item.id">{{ item.id }}</EntityLink></td>
                <td><EntityLink type="group" :id="item.groupId">{{ item.groupId }}</EntityLink></td>
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
