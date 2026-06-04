<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { getOperationLogsApi, type OperationLogItem } from "../api/operationLog";
import { useI18n } from "../i18n";
import { useAuthStore } from "../stores/auth";

type ResultFilterValue = "" | "0" | "1";

const { t } = useI18n();
const authStore = useAuthStore();

const loading = ref(false);
const list = ref<OperationLogItem[]>([]);
const errorMessage = ref("");
const page = ref(1);
const size = ref(20);
const total = ref(0);
const startTime = ref("");
const endTime = ref("");
const operator = ref("");
const moduleKeyword = ref("");
const result = ref<ResultFilterValue>("");

const isAdmin = computed(() => Boolean(authStore.profile?.isAdmin));
const emptyText = computed(() => (loading.value ? t("operationLog.emptyLoading") : t("operationLog.empty")));
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));
const pagerText = computed(() =>
  t("operationLog.pager", { page: page.value, totalPages: totalPages.value, total: total.value })
);
const resultOptions = computed(() => [
  { label: t("operationLog.result.all"), value: "" },
  { label: t("operationLog.result.success"), value: "0" },
  { label: t("operationLog.result.failed"), value: "1" }
]);

function normalizeDateTimeForQuery(value: string): string | undefined {
  const trimmed = value.trim();
  if (!trimmed) {
    return undefined;
  }
  return `${trimmed.replace("T", " ")}:00`;
}

function getOperatorLabel(item: OperationLogItem): string {
  return item.operatorName || item.operatorUsername || item.operator || "-";
}

function getModuleLabel(item: OperationLogItem): string {
  return item.moduleName || item.moduleCode || "-";
}

function getFailureReason(item: OperationLogItem): string {
  return item.failureReason || item.errorMessage || "-";
}

function getResultLabel(value: OperationLogItem["resultStatus"]): string {
  if (value === false || value === 0) {
    return t("operationLog.result.success");
  }
  if (value === true || value === 1) {
    return t("operationLog.result.failed");
  }

  const normalized = String(value || "").trim().toUpperCase();
  if (["SUCCESS", "SUCCEEDED", "PASS", "PASSED", "OK"].includes(normalized)) {
    return t("operationLog.result.success");
  }
  if (["FAILED", "FAIL", "ERROR"].includes(normalized)) {
    return t("operationLog.result.failed");
  }
  return value == null || value === "" ? "-" : String(value);
}

function getResultTagClass(value: OperationLogItem["resultStatus"]): string {
  const normalized = String(value || "").trim().toUpperCase();
  if (value === false || value === 0 || ["SUCCESS", "SUCCEEDED", "PASS", "PASSED", "OK"].includes(normalized)) {
    return "status-tag status-tag--success";
  }
  if (value === true || value === 1 || ["FAILED", "FAIL", "ERROR"].includes(normalized)) {
    return "status-tag status-tag--danger";
  }
  return "status-tag status-tag--neutral";
}

async function loadData(): Promise<void> {
  if (!isAdmin.value) {
    list.value = [];
    total.value = 0;
    return;
  }

  loading.value = true;
  errorMessage.value = "";
  try {
    const data = await getOperationLogsApi({
      page: page.value,
      size: size.value,
      startTime: normalizeDateTimeForQuery(startTime.value),
      endTime: normalizeDateTimeForQuery(endTime.value),
      operator: operator.value.trim() || undefined,
      moduleCode: moduleKeyword.value.trim() || undefined,
      resultStatus: result.value || undefined
    });
    list.value = data.list || [];
    total.value = data.total || 0;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("operationLog.loadFailed");
  } finally {
    loading.value = false;
  }
}

function applyFilter(): void {
  page.value = 1;
  void loadData();
}

function resetFilters(): void {
  startTime.value = "";
  endTime.value = "";
  operator.value = "";
  moduleKeyword.value = "";
  result.value = "";
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

onMounted(() => {
  void loadData();
});
</script>

<template>
  <section class="page-card">
    <div v-if="!isAdmin" class="empty">{{ t("operationLog.noAccess") }}</div>
    <template v-else>
      <header class="page-toolbar">
        <h1>{{ t("operationLog.title") }}</h1>
        <div class="actions">
          <button class="btn btn--subtle" :disabled="loading" @click="refresh">{{ t("common.refresh") }}</button>
        </div>
      </header>

      <div class="operation-log-filters">
        <label class="operation-log-filters__field">
          <span>{{ t("operationLog.filters.startTime") }}</span>
          <input v-model="startTime" type="datetime-local" :disabled="loading" />
        </label>
        <label class="operation-log-filters__field">
          <span>{{ t("operationLog.filters.endTime") }}</span>
          <input v-model="endTime" type="datetime-local" :disabled="loading" />
        </label>
        <label class="operation-log-filters__field">
          <span>{{ t("operationLog.filters.operator") }}</span>
          <input
            v-model="operator"
            type="text"
            :placeholder="t('operationLog.filters.operatorPlaceholder')"
            :disabled="loading"
            @keyup.enter="applyFilter"
          />
        </label>
        <label class="operation-log-filters__field">
          <span>{{ t("operationLog.filters.module") }}</span>
          <input
            v-model="moduleKeyword"
            type="text"
            :placeholder="t('operationLog.filters.modulePlaceholder')"
            :disabled="loading"
            @keyup.enter="applyFilter"
          />
        </label>
        <label class="operation-log-filters__field">
          <span>{{ t("operationLog.filters.result") }}</span>
          <select v-model="result" :disabled="loading">
            <option v-for="item in resultOptions" :key="item.value || 'all'" :value="item.value">
              {{ item.label }}
            </option>
          </select>
        </label>
        <div class="operation-log-filters__actions">
          <button class="btn btn--primary" :disabled="loading" @click="applyFilter">
            {{ t("operationLog.actions.search") }}
          </button>
          <button class="btn btn--subtle" :disabled="loading" @click="resetFilters">
            {{ t("operationLog.actions.reset") }}
          </button>
        </div>
      </div>

      <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
      <div v-if="loading" class="empty">{{ emptyText }}</div>
      <div v-else-if="!list.length" class="empty">{{ emptyText }}</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>{{ t("operationLog.columns.time") }}</th>
              <th>{{ t("operationLog.columns.operator") }}</th>
              <th>{{ t("operationLog.columns.module") }}</th>
              <th>{{ t("operationLog.columns.button") }}</th>
              <th>{{ t("operationLog.columns.result") }}</th>
              <th>{{ t("operationLog.columns.content") }}</th>
              <th>{{ t("operationLog.columns.failureReason") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.operateTime || "-" }}</td>
              <td>{{ getOperatorLabel(item) }}</td>
              <td>{{ getModuleLabel(item) }}</td>
              <td>{{ item.buttonName || "-" }}</td>
              <td><span :class="getResultTagClass(item.resultStatus)">{{ getResultLabel(item.resultStatus) }}</span></td>
              <td class="operation-log__content" :title="item.content || '-'">{{ item.content || "-" }}</td>
              <td class="operation-log__content" :title="getFailureReason(item)">{{ getFailureReason(item) }}</td>
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
    </template>
  </section>
</template>

<style scoped>
.operation-log-filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.operation-log-filters__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: var(--ea-text-muted);
  font-size: 13px;
}

.operation-log-filters__field input,
.operation-log-filters__field select {
  height: 34px;
  border: 1px solid var(--ea-border);
  border-radius: 8px;
  padding: 0 10px;
  background: #fff;
  color: var(--ea-text);
}

.operation-log-filters__actions {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

.operation-log__content {
  max-width: 320px;
  white-space: normal;
  word-break: break-word;
}
</style>
