<script setup lang="ts">
import {
  createDatasourceApi,
  type Datasource,
  type DatasourcePayload,
  type DatasourceQuery,
  type DatasourceTypeOption,
  getDatasourcesApi,
  getDatasourceTypesApi,
  testDatasourceConnectionApi,
  updateDatasourceApi,
  updateDatasourceStatusApi
} from "../api/datasource";
import DatasourceFormDialog from "../components/DatasourceFormDialog.vue";
import {
  datasourceStatusDictionary,
  getStatusLabel,
  getStatusTagClass
} from "../utils/dictionaries";
import { computed, ref } from "vue";
import { useI18n } from "../i18n";
import { useAuthStore } from "../stores/auth";

const loading = ref(false);
const list = ref<Datasource[]>([]);
const datasourceTypes = ref<DatasourceTypeOption[]>([]);
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
const busyRows = ref(new Set<number>());
const busyActions = ref(new Set<string>());

const page = ref(1);
const size = ref(20);
const total = ref(0);
const keywordFilter = ref("");
const statusFilter = ref<number | undefined>(undefined);

const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogSubmitting = ref(false);
const activeItem = ref<Datasource | null>(null);
const { t } = useI18n();
const authStore = useAuthStore();

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / size.value)));
const pagerText = computed(() =>
  t("datasource.pager", { page: page.value, totalPages: totalPages.value, total: total.value })
);
const statusOptions = computed(() => [
  { label: t("task.filters.all"), value: undefined },
  { label: t("status.untested"), value: 0 },
  { label: t("status.enabled"), value: 1 },
  { label: t("status.disabled"), value: 2 }
]);
const getActionKey = (action: string, id: number): string => `${action}:${id}`;
const isRowBusy = (id: number): boolean => busyRows.value.has(id);
const isActionBusy = (action: string, id: number): boolean => busyActions.value.has(getActionKey(action, id));

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const query: DatasourceQuery = {
      page: page.value,
      size: size.value,
      keyword: keywordFilter.value || undefined,
      status: statusFilter.value
    };
    const [result, datasourceTypeList] = await Promise.all([
      getDatasourcesApi(query),
      getDatasourceTypesApi()
    ]);
    list.value = result.data;
    total.value = result.total;
    datasourceTypes.value = datasourceTypeList;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("datasource.loadFailed");
  } finally {
    loading.value = false;
  }
}

function handleQuery(): void {
  page.value = 1;
  void loadData();
}

function handleReset(): void {
  keywordFilter.value = "";
  statusFilter.value = undefined;
  page.value = 1;
  void loadData();
}

function handlePageChange(newPage: number): void {
  page.value = newPage;
  void loadData();
}

function handleSizeChange(newSize: number): void {
  size.value = newSize;
  page.value = 1;
  void loadData();
}

function openCreate(): void {
  dialogMode.value = "create";
  activeItem.value = null;
  dialogVisible.value = true;
}

function openEdit(item: Datasource): void {
  dialogMode.value = "edit";
  activeItem.value = item;
  dialogVisible.value = true;
}

async function submitForm(payload: DatasourcePayload): Promise<void> {
  if (dialogSubmitting.value) {
    return;
  }
  dialogSubmitting.value = true;
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    if (dialogMode.value === "create") {
      await createDatasourceApi(payload);
      successMessage.value = t("datasource.created");
    } else if (activeItem.value) {
      await updateDatasourceApi(activeItem.value.id, payload);
      successMessage.value = t("datasource.updated");
    }
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("datasource.saveFailed");
  } finally {
    dialogSubmitting.value = false;
  }
}

async function toggleStatus(item: Datasource): Promise<void> {
  if (isRowBusy(item.id) || isActionBusy("toggleStatus", item.id)) {
    return;
  }
  const nextStatus = 2;
  const actionKey = getActionKey("toggleStatus", item.id);
  busyRows.value.add(item.id);
  busyActions.value.add(actionKey);
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    await updateDatasourceStatusApi(item.id, nextStatus);
    successMessage.value = t("datasource.statusUpdated");
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("datasource.statusUpdateFailed");
  } finally {
    busyActions.value.delete(actionKey);
    busyRows.value.delete(item.id);
  }
}

async function testConnection(item: Datasource): Promise<void> {
  if (isRowBusy(item.id) || isActionBusy("testConnection", item.id)) {
    return;
  }
  const actionKey = getActionKey("testConnection", item.id);
  busyRows.value.add(item.id);
  busyActions.value.add(actionKey);
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    const success = await testDatasourceConnectionApi({
      id: item.id,
      datasourceCode: item.datasourceCode,
      datasourceName: item.datasourceName,
      datasourceType: item.datasourceType,
      jdbcUrl: item.jdbcUrl,
      username: item.username,
      ownerUserId: item.ownerUserId,
      remark: item.remark,
      status: item.status
    });
    if (success) {
      successMessage.value = t("datasource.testedAndEnabled");
    } else {
      actionErrorMessage.value = `${t("datasource.connectionTestFailed")} ${t("datasource.connectionTip")}`;
    }
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("datasource.connectionTestFailed");
  } finally {
    busyActions.value.delete(actionKey);
    busyRows.value.delete(item.id);
  }
}

void loadData();
</script>

<template>
  <section class="page-card">
    <header class="page-toolbar">
      <h1>{{ t("datasource.title") }}</h1>
      <div class="actions">
        <input
          v-model="keywordFilter"
          type="text"
          :placeholder="t('datasource.keywordPlaceholder')"
          class="filter-bar__input"
        />
        <select v-model="statusFilter" class="filter-bar__select">
          <option v-for="item in statusOptions" :key="String(item.value)" :value="item.value">
            {{ item.label }}
          </option>
        </select>
        <button class="btn btn--subtle" :disabled="loading" @click="handleQuery">{{ t("datasource.query") }}</button>
        <button class="btn btn--subtle" :disabled="loading" @click="handleReset">{{ t("datasource.reset") }}</button>
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
        <button v-if="authStore.hasCapability('DATASOURCE_CREATE')" class="btn btn--primary" :disabled="loading" @click="openCreate">{{ t("datasource.new") }}</button>
      </div>
    </header>
    <p v-if="successMessage" class="feedback">{{ successMessage }}</p>
    <p class="hint">{{ t("datasource.connectionTip") }}</p>
    <p v-if="actionErrorMessage" class="error">{{ actionErrorMessage }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
    <div v-if="loading" class="empty">{{ emptyText }}</div>
    <div v-else-if="!list.length" class="empty">{{ emptyText }}</div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>{{ t("datasource.columns.code") }}</th>
            <th>{{ t("datasource.columns.name") }}</th>
            <th>{{ t("datasource.columns.type") }}</th>
            <th>{{ t("datasource.columns.jdbcUrl") }}</th>
            <th>{{ t("datasource.columns.username") }}</th>
            <th>{{ t("datasource.columns.password") }}</th>
            <th>{{ t("datasource.columns.status") }}</th>
            <th>{{ t("datasource.columns.actions") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.datasourceCode }}</td>
            <td>{{ item.datasourceName }}</td>
            <td>{{ item.datasourceType }}</td>
            <td class="truncate" :title="item.jdbcUrl">{{ item.jdbcUrl }}</td>
            <td>{{ item.username }}</td>
            <td>{{ item.passwordCipher || "****" }}</td>
            <td><span :class="getStatusTagClass(datasourceStatusDictionary, item.status)">{{ getStatusLabel(datasourceStatusDictionary, item.status) }}</span></td>
            <td class="row-actions">
              <button v-if="authStore.hasCapability('DATASOURCE_EDIT_AUTHORIZED')" class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="openEdit(item)">{{ t("common.edit") }}</button>
              <button v-if="authStore.hasCapability('DATASOURCE_EDIT_AUTHORIZED')" class="btn btn--subtle" :disabled="isRowBusy(item.id) || item.status !== 1" @click="toggleStatus(item)">
                {{ t("common.disable") }}
              </button>
              <button v-if="authStore.hasCapability('DATASOURCE_EDIT_AUTHORIZED')" class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="testConnection(item)">
                {{ isActionBusy("testConnection", item.id) ? t("common.testing") : t("common.test") }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <footer v-if="total > 0" class="pager">
      <span>{{ pagerText }}</span>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="page <= 1 || loading" @click="handlePageChange(page - 1)">{{ t("common.prev") }}</button>
        <button class="btn btn--subtle" :disabled="page >= totalPages || loading" @click="handlePageChange(page + 1)">{{ t("common.next") }}</button>
        <select :value="size" class="pager__size" @change="handleSizeChange(Number(($event.target as HTMLSelectElement).value))">
          <option :value="10">10</option>
          <option :value="20">20</option>
          <option :value="50">50</option>
        </select>
      </div>
    </footer>

    <DatasourceFormDialog
      :visible="dialogVisible"
      :mode="dialogMode"
      :datasource-types="datasourceTypes"
      :initial-value="activeItem"
      :submitting="dialogSubmitting"
      @close="dialogVisible = false"
      @submit="submitForm"
    />
  </section>
</template>
