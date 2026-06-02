<script setup lang="ts">
import {
  createArchiveGroupApi,
  deleteArchiveGroupApi,
  type ArchiveGroup,
  type ArchiveGroupPayload,
  getArchiveGroupsApi,
  triggerArchiveGroupApi,
  updateArchiveGroupApi,
  updateArchiveGroupStatusApi
} from "../api/archiveGroup";
import {
  createArchiveGroupItemByIdApi,
  createArchiveGroupItemByTimeApi,
  deleteArchiveGroupItemByIdApi,
  deleteArchiveGroupItemByTimeApi,
  getArchiveGroupItemByIdApi,
  getArchiveGroupItemByTimeApi,
  getArchiveGroupItemsApi,
  type ArchiveGroupItemById,
  type ArchiveGroupItemByIdPayload,
  type ArchiveGroupItemByTime,
  type ArchiveGroupItemByTimePayload,
  type ArchiveGroupItemSummary,
  updateArchiveGroupItemByIdApi,
  updateArchiveGroupItemByIdStatusApi,
  updateArchiveGroupItemByTimeApi,
  updateArchiveGroupItemByTimeStatusApi
} from "../api/archiveGroupItem";
import { getDatasourcesApi, type Datasource } from "../api/datasource";
import ArchiveGroupFormDialog from "../components/ArchiveGroupFormDialog.vue";
import ArchiveGroupItemByIdFormDialog from "../components/ArchiveGroupItemByIdFormDialog.vue";
import ArchiveGroupItemByTimeFormDialog from "../components/ArchiveGroupItemByTimeFormDialog.vue";
import { archiveEnableStatusDictionary, getStatusLabel, getStatusTagClass } from "../utils/dictionaries";
import { computed, ref } from "vue";
import { useI18n } from "../i18n";

const loading = ref(false);
const itemLoading = ref(false);
const groups = ref<ArchiveGroup[]>([]);
const datasources = ref<Datasource[]>([]);
const items = ref<ArchiveGroupItemSummary[]>([]);
const selectedGroupId = ref<number | null>(null);
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
const busyRows = ref(new Set<number>());
const busyActions = ref(new Set<string>());

const groupDialogVisible = ref(false);
const groupDialogMode = ref<"create" | "edit">("create");
const groupDialogSubmitting = ref(false);
const activeGroup = ref<ArchiveGroup | null>(null);

const idDialogVisible = ref(false);
const timeDialogVisible = ref(false);
const itemDialogMode = ref<"create" | "edit">("create");
const itemDialogSubmitting = ref(false);
const activeIdItem = ref<ArchiveGroupItemById | null>(null);
const activeTimeItem = ref<ArchiveGroupItemByTime | null>(null);
const { t } = useI18n();

const selectedGroup = computed(() => groups.value.find((item) => item.id === selectedGroupId.value) || null);
const groupEmptyText = computed(() => (loading.value ? t("archiveGroup.emptyLoading") : t("archiveGroup.empty")));
const itemEmptyText = computed(() => (itemLoading.value ? t("archiveGroup.item.emptyLoading") : t("archiveGroup.item.empty")));

const getActionKey = (action: string, id: number): string => `${action}:${id}`;
const isRowBusy = (id: number): boolean => busyRows.value.has(id);
const isActionBusy = (action: string, id: number): boolean => busyActions.value.has(getActionKey(action, id));
const datasourceName = (id: number): string => datasources.value.find((item) => item.id === id)?.datasourceName || String(id);

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [groupResult, datasourceResult] = await Promise.all([getArchiveGroupsApi(), getDatasourcesApi()]);
    groups.value = groupResult;
    datasources.value = datasourceResult;
    if (!selectedGroupId.value && groups.value.length) {
      selectedGroupId.value = groups.value[0].id;
    }
    if (selectedGroupId.value) {
      await loadItems(selectedGroupId.value);
    } else {
      items.value = [];
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("archiveGroup.loadFailed");
  } finally {
    loading.value = false;
  }
}

async function loadItems(groupId: number): Promise<void> {
  itemLoading.value = true;
  actionErrorMessage.value = "";
  try {
    items.value = await getArchiveGroupItemsApi(groupId);
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("archiveGroup.item.loadFailed");
  } finally {
    itemLoading.value = false;
  }
}

function selectGroup(group: ArchiveGroup): void {
  selectedGroupId.value = group.id;
  void loadItems(group.id);
}

function openCreateGroup(): void {
  groupDialogMode.value = "create";
  activeGroup.value = null;
  groupDialogVisible.value = true;
}

function openEditGroup(group: ArchiveGroup): void {
  groupDialogMode.value = "edit";
  activeGroup.value = group;
  groupDialogVisible.value = true;
}

async function submitGroup(payload: ArchiveGroupPayload): Promise<void> {
  if (groupDialogSubmitting.value) {
    return;
  }
  groupDialogSubmitting.value = true;
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    if (groupDialogMode.value === "create") {
      const created = await createArchiveGroupApi(payload);
      selectedGroupId.value = created.id;
      successMessage.value = t("archiveGroup.created");
    } else if (activeGroup.value) {
      await updateArchiveGroupApi(activeGroup.value.id, payload);
      successMessage.value = t("archiveGroup.updated");
    }
    groupDialogVisible.value = false;
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("archiveGroup.saveFailed");
  } finally {
    groupDialogSubmitting.value = false;
  }
}

async function toggleGroupStatus(group: ArchiveGroup): Promise<void> {
  await runGroupAction("toggleStatus", group.id, async () => {
    await updateArchiveGroupStatusApi(group.id, group.enableStatus === 0 ? 1 : 0);
    successMessage.value = t("archiveGroup.statusUpdated");
    await loadData();
  });
}

async function deleteGroup(group: ArchiveGroup): Promise<void> {
  if (!window.confirm(t("archiveGroup.deleteConfirm"))) {
    return;
  }
  await runGroupAction("delete", group.id, async () => {
    await deleteArchiveGroupApi(group.id);
    if (selectedGroupId.value === group.id) {
      selectedGroupId.value = null;
    }
    successMessage.value = t("archiveGroup.deleted");
    await loadData();
  });
}

async function triggerGroup(group: ArchiveGroup): Promise<void> {
  await runGroupAction("trigger", group.id, async () => {
    const task = await triggerArchiveGroupApi(group.id);
    successMessage.value = t("archiveGroup.triggered").replace("{id}", String(task.id));
    await loadData();
  });
}

async function runGroupAction(action: string, id: number, handler: () => Promise<void>): Promise<void> {
  if (isRowBusy(id) || isActionBusy(action, id)) {
    return;
  }
  const actionKey = getActionKey(action, id);
  busyRows.value.add(id);
  busyActions.value.add(actionKey);
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    await handler();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("archiveGroup.actionFailed");
  } finally {
    busyActions.value.delete(actionKey);
    busyRows.value.delete(id);
  }
}

function openCreateIdItem(): void {
  itemDialogMode.value = "create";
  activeIdItem.value = null;
  idDialogVisible.value = true;
}

function openCreateTimeItem(): void {
  itemDialogMode.value = "create";
  activeTimeItem.value = null;
  timeDialogVisible.value = true;
}

async function openEditItem(item: ArchiveGroupItemSummary): Promise<void> {
  if (!selectedGroupId.value) {
    return;
  }
  itemDialogMode.value = "edit";
  actionErrorMessage.value = "";
  try {
    if (item.itemType === "ID") {
      activeIdItem.value = await getArchiveGroupItemByIdApi(selectedGroupId.value, item.id);
      idDialogVisible.value = true;
    } else {
      activeTimeItem.value = await getArchiveGroupItemByTimeApi(selectedGroupId.value, item.id);
      timeDialogVisible.value = true;
    }
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("archiveGroup.item.loadFailed");
  }
}

async function submitIdItem(payload: ArchiveGroupItemByIdPayload): Promise<void> {
  if (!selectedGroupId.value || itemDialogSubmitting.value) {
    return;
  }
  itemDialogSubmitting.value = true;
  try {
    if (itemDialogMode.value === "create") {
      await createArchiveGroupItemByIdApi(selectedGroupId.value, payload);
    } else if (activeIdItem.value) {
      await updateArchiveGroupItemByIdApi(selectedGroupId.value, activeIdItem.value.id, payload);
    }
    idDialogVisible.value = false;
    successMessage.value = t("archiveGroup.item.saved");
    await loadItems(selectedGroupId.value);
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("archiveGroup.item.saveFailed");
  } finally {
    itemDialogSubmitting.value = false;
  }
}

async function submitTimeItem(payload: ArchiveGroupItemByTimePayload): Promise<void> {
  if (!selectedGroupId.value || itemDialogSubmitting.value) {
    return;
  }
  itemDialogSubmitting.value = true;
  try {
    if (itemDialogMode.value === "create") {
      await createArchiveGroupItemByTimeApi(selectedGroupId.value, payload);
    } else if (activeTimeItem.value) {
      await updateArchiveGroupItemByTimeApi(selectedGroupId.value, activeTimeItem.value.id, payload);
    }
    timeDialogVisible.value = false;
    successMessage.value = t("archiveGroup.item.saved");
    await loadItems(selectedGroupId.value);
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("archiveGroup.item.saveFailed");
  } finally {
    itemDialogSubmitting.value = false;
  }
}

async function toggleItemStatus(item: ArchiveGroupItemSummary): Promise<void> {
  if (!selectedGroupId.value) {
    return;
  }
  const nextStatus = item.enableStatus === 0 ? 1 : 0;
  if (item.itemType === "ID") {
    await updateArchiveGroupItemByIdStatusApi(selectedGroupId.value, item.id, nextStatus);
  } else {
    await updateArchiveGroupItemByTimeStatusApi(selectedGroupId.value, item.id, nextStatus);
  }
  successMessage.value = t("archiveGroup.item.statusUpdated");
  await loadItems(selectedGroupId.value);
}

async function deleteItem(item: ArchiveGroupItemSummary): Promise<void> {
  if (!selectedGroupId.value || !window.confirm(t("archiveGroup.item.deleteConfirm"))) {
    return;
  }
  if (item.itemType === "ID") {
    await deleteArchiveGroupItemByIdApi(selectedGroupId.value, item.id);
  } else {
    await deleteArchiveGroupItemByTimeApi(selectedGroupId.value, item.id);
  }
  successMessage.value = t("archiveGroup.item.deleted");
  await loadItems(selectedGroupId.value);
}

void loadData();
</script>

<template>
  <section class="page-card">
    <header class="page-toolbar">
      <h1>{{ t("archiveGroup.title") }}</h1>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
        <button class="btn btn--primary" :disabled="loading" @click="openCreateGroup">{{ t("archiveGroup.new") }}</button>
      </div>
    </header>

    <p v-if="successMessage" class="feedback">{{ successMessage }}</p>
    <p v-if="actionErrorMessage" class="error">{{ actionErrorMessage }}</p>
    <p v-if="errorMessage" class="error">{{ errorMessage }}</p>

    <div v-if="loading" class="empty">{{ groupEmptyText }}</div>
    <div v-else-if="!groups.length" class="empty">{{ groupEmptyText }}</div>
    <div v-else class="table-wrap">
      <table class="table">
        <thead>
          <tr>
            <th>{{ t("archiveGroup.columns.code") }}</th>
            <th>{{ t("archiveGroup.columns.name") }}</th>
            <th>{{ t("archiveGroup.columns.source") }}</th>
            <th>{{ t("archiveGroup.columns.target") }}</th>
            <th>{{ t("archiveGroup.columns.status") }}</th>
            <th>{{ t("archiveGroup.columns.actions") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="group in groups" :key="group.id" :class="{ selected: selectedGroupId === group.id }">
            <td>{{ group.groupCode }}</td>
            <td>{{ group.groupName }}</td>
            <td>{{ datasourceName(group.sourceDatasourceId) }}</td>
            <td>{{ datasourceName(group.targetDatasourceId) }}</td>
            <td>
              <span :class="getStatusTagClass(archiveEnableStatusDictionary, group.enableStatus)">
                {{ getStatusLabel(archiveEnableStatusDictionary, group.enableStatus) }}
              </span>
            </td>
            <td class="row-actions">
              <button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="selectGroup(group)">{{ t("archiveGroup.items") }}</button>
              <button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="openEditGroup(group)">{{ t("common.edit") }}</button>
              <button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="toggleGroupStatus(group)">
                {{ group.enableStatus === 0 ? t("common.disable") : t("common.enable") }}
              </button>
              <button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="triggerGroup(group)">{{ t("archiveGroup.trigger") }}</button>
              <button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click="deleteGroup(group)">{{ t("common.delete") }}</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <section class="page-card archive-items">
      <header class="page-toolbar">
        <h2>{{ selectedGroup ? selectedGroup.groupName : t("archiveGroup.item.title") }}</h2>
        <div class="actions">
          <button class="btn btn--subtle" :disabled="!selectedGroupId || itemLoading" @click="selectedGroupId && loadItems(selectedGroupId)">
            {{ t("common.refresh") }}
          </button>
          <button class="btn btn--primary" :disabled="!selectedGroupId" @click="openCreateIdItem">{{ t("archiveGroup.item.newId") }}</button>
          <button class="btn btn--primary" :disabled="!selectedGroupId" @click="openCreateTimeItem">{{ t("archiveGroup.item.newTime") }}</button>
        </div>
      </header>
      <div v-if="itemLoading" class="empty">{{ itemEmptyText }}</div>
      <div v-else-if="!items.length" class="empty">{{ itemEmptyText }}</div>
      <div v-else class="table-wrap">
        <table class="table">
          <thead>
            <tr>
              <th>{{ t("archiveGroup.item.type") }}</th>
              <th>{{ t("archiveGroup.item.sourceTable") }}</th>
              <th>{{ t("archiveGroup.item.targetTable") }}</th>
              <th>{{ t("archiveGroup.item.priority") }}</th>
              <th>{{ t("archiveGroup.item.stepCount") }}</th>
              <th>{{ t("archiveGroup.item.status") }}</th>
              <th>{{ t("common.actions") }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in items" :key="`${item.itemType}:${item.id}`">
              <td>{{ item.itemType }}</td>
              <td>{{ item.sourceTable }}</td>
              <td>{{ item.targetTable }}</td>
              <td>{{ item.priority }}</td>
              <td>{{ item.stepCount || "-" }}</td>
              <td>
                <span :class="getStatusTagClass(archiveEnableStatusDictionary, item.enableStatus)">
                  {{ getStatusLabel(archiveEnableStatusDictionary, item.enableStatus) }}
                </span>
              </td>
              <td class="row-actions">
                <button class="btn btn--subtle" @click="openEditItem(item)">{{ t("common.edit") }}</button>
                <button class="btn btn--subtle" @click="toggleItemStatus(item)">
                  {{ item.enableStatus === 0 ? t("common.disable") : t("common.enable") }}
                </button>
                <button class="btn btn--subtle" @click="deleteItem(item)">{{ t("common.delete") }}</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <ArchiveGroupFormDialog
      :visible="groupDialogVisible"
      :mode="groupDialogMode"
      :initial-value="activeGroup"
      :datasources="datasources"
      :submitting="groupDialogSubmitting"
      @close="groupDialogVisible = false"
      @submit="submitGroup"
    />
    <ArchiveGroupItemByIdFormDialog
      :visible="idDialogVisible"
      :mode="itemDialogMode"
      :initial-value="activeIdItem"
      :submitting="itemDialogSubmitting"
      @close="idDialogVisible = false"
      @submit="submitIdItem"
    />
    <ArchiveGroupItemByTimeFormDialog
      :visible="timeDialogVisible"
      :mode="itemDialogMode"
      :initial-value="activeTimeItem"
      :submitting="itemDialogSubmitting"
      @close="timeDialogVisible = false"
      @submit="submitTimeItem"
    />
  </section>
</template>

<style scoped>
.selected {
  background: rgba(40, 120, 96, 0.08);
}

.archive-items {
  margin-top: 24px;
  box-shadow: none;
}
</style>
