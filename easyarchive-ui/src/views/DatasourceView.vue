<script setup lang="ts">
import {
  createDatasourceApi,
  type Datasource,
  type DatasourcePayload,
  getDatasourcesApi,
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

const loading = ref(false);
const list = ref<Datasource[]>([]);
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
const busyRows = ref(new Set<number>());
const busyActions = ref(new Set<string>());

const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogSubmitting = ref(false);
const activeItem = ref<Datasource | null>(null);
const { t } = useI18n();

const emptyText = computed(() => (loading.value ? t("datasource.emptyLoading") : t("datasource.empty")));
const getActionKey = (action: string, id: number): string => `${action}:${id}`;
const isRowBusy = (id: number): boolean => busyRows.value.has(id);
const isActionBusy = (action: string, id: number): boolean => busyActions.value.has(getActionKey(action, id));

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    list.value = await getDatasourcesApi();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("datasource.loadFailed");
  } finally {
    loading.value = false;
  }
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
  const nextStatus = item.status === 1 ? 0 : 1;
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
    actionErrorMessage.value = "";
    actionErrorMessage.value = t("datasource.connectionTip");
    openEdit(item);
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
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
        <button class="btn btn--primary" :disabled="loading" @click="openCreate">{{ t("datasource.new") }}</button>
      </div>
    </header>
    <p v-if="successMessage" class="feedback">{{ successMessage }}</p>
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
            <td><span :class="getStatusTagClass(datasourceStatusDictionary, item.status)">{{ getStatusLabel(datasourceStatusDictionary, item.status) }}</span></td>
            <td class="row-actions">
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="openEdit(item)">{{ t("common.edit") }}</button>
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="toggleStatus(item)">
                {{ item.status === 1 ? t("common.disable") : t("common.enable") }}
              </button>
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="testConnection(item)">
                {{ isActionBusy("testConnection", item.id) ? t("common.testing") : t("common.test") }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <DatasourceFormDialog
      :visible="dialogVisible"
      :mode="dialogMode"
      :initial-value="activeItem"
      :submitting="dialogSubmitting"
      @close="dialogVisible = false"
      @submit="submitForm"
    />
  </section>
</template>
