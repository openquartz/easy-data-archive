<script setup lang="ts">
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  deleteArchiveGroupApi,
  getArchiveGroupOverviewApi,
  triggerArchiveGroupApi,
  type ArchiveGroup,
  type ArchiveGroupOverview,
  type ArchiveGroupPayload,
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
import ArchiveGroupItemByIdFormDialog from "../components/ArchiveGroupItemByIdFormDialog.vue";
import ArchiveGroupItemByTimeFormDialog from "../components/ArchiveGroupItemByTimeFormDialog.vue";
import ArchiveGroupFormDialog from "../components/ArchiveGroupFormDialog.vue";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import { useI18n } from "../i18n";
import EntityLink from "../components/EntityLink.vue";
import { getDatasourcesApi, type Datasource } from "../api/datasource";
import { getUsersApiSilent, type User } from "../api/user";
import { archiveEnableStatusDictionary, getStatusLabel, getStatusTagClass, taskStatusDictionary } from "../utils/dictionaries";
import { formatArchiveGroupItemRange } from "../utils/archiveGroupItemRange";
import { useAuthStore } from "../stores/auth";
import {
  getArchiveGroupRuntimeProcessedRecords,
  canTriggerArchiveGroup,
  hasArchiveGroupActiveTask,
  resolveArchiveGroupRuntimeProgress,
  shouldShowArchiveGroupTriggerAction
} from "../utils/archiveGroupRuntime";

const route = useRoute();
const router = useRouter();
const { t } = useI18n();
const authStore = useAuthStore();

const loading = ref(false);
const errorMessage = ref("");
const successMessage = ref("");
const notFound = ref(false);
const group = ref<ArchiveGroup | null>(null);
const items = ref<ArchiveGroupItemSummary[]>([]);
const overview = ref<ArchiveGroupOverview | null>(null);
const busyItemIds = ref(new Set<number>());
const idDialogVisible = ref(false);
const timeDialogVisible = ref(false);
const idDialogMode = ref<"create" | "edit">("create");
const timeDialogMode = ref<"create" | "edit">("create");
const idDialogReadonly = ref(false);
const timeDialogReadonly = ref(false);
const itemDialogSubmitting = ref(false);
const activeIdItem = ref<ArchiveGroupItemById | null>(null);
const activeTimeItem = ref<ArchiveGroupItemByTime | null>(null);
const groupDialogVisible = ref(false);
const groupDialogSubmitting = ref(false);
let loadToken = 0;
const datasources = ref<Datasource[]>([]);
const users = ref<User[]>([]);
const busyGroupAction = ref(false);

const groupId = computed(() => Number(route.params.id));
const formattedLastExecuteTime = computed(() => formatTimestamp(overview.value?.taskStats.lastExecuteTime));
const enabledDatasources = computed(() => datasources.value.filter((item) => item.status === 1));

function formatTimestamp(value?: number): string {
  if (typeof value !== "number" || !Number.isFinite(value) || value <= 0) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "-";
  }
  return date.toLocaleString();
}

function formatSwitchFlag(value?: number): string {
  if (value === 0) {
    return t("common.yes");
  }
  if (value === 1) {
    return t("common.no");
  }
  return "-";
}

function datasourceName(id: number): string {
  return datasources.value.find((item) => item.id === id)?.datasourceName || String(id);
}

function formatNotifyChannel(channel?: ArchiveGroup["notifyChannel"]): string {
  if (channel === "IN_APP") {
    return t("archiveGroup.form.notifyChannels.inApp");
  }
  if (channel === "FEISHU") {
    return t("archiveGroup.form.notifyChannels.feishu");
  }
  if (channel === "WECOM") {
    return t("archiveGroup.form.notifyChannels.wecom");
  }
  return "-";
}

async function syncTitle(title: string, token: number): Promise<void> {
  if (token !== loadToken || route.query.title === title) {
    return;
  }
  await router.replace({
    query: {
      ...route.query,
      title
    }
  });
}

function isCurrentToken(token: number): boolean {
  return token === loadToken;
}

function isItemBusy(itemId: number): boolean {
  return busyItemIds.value.has(itemId);
}

function resetItemDialogs(): void {
  idDialogVisible.value = false;
  timeDialogVisible.value = false;
  idDialogReadonly.value = false;
  timeDialogReadonly.value = false;
  activeIdItem.value = null;
  activeTimeItem.value = null;
  itemDialogSubmitting.value = false;
}

async function loadDetail(): Promise<void> {
  const token = ++loadToken;
  loading.value = true;

  if (!Number.isFinite(groupId.value) || groupId.value <= 0) {
    if (!isCurrentToken(token)) {
      return;
    }
    group.value = null;
    items.value = [];
    overview.value = null;
    notFound.value = true;
    errorMessage.value = t("archiveGroupDetail.notFound");
    loading.value = false;
    await syncTitle(t("archiveGroupDetail.title"), token);
    return;
  }

  if (!isCurrentToken(token)) {
    return;
  }
  notFound.value = false;
  errorMessage.value = "";

  try {
    const [itemsResult, overviewResult, datasourceResult, userResult] = await Promise.all([
      getArchiveGroupItemsApi(groupId.value),
      getArchiveGroupOverviewApi(groupId.value),
      getDatasourcesApi({ page: 1, size: 1000 }).then((r) => r.data),
      getUsersApiSilent().catch(() => [] as User[])
    ]);

    if (!isCurrentToken(token)) {
      return;
    }

    group.value = overviewResult.group || null;
    items.value = itemsResult;
    overview.value = overviewResult;
    datasources.value = datasourceResult;
    users.value = userResult;

    const groupName = overviewResult.group?.groupName;
    if (groupName) {
      await syncTitle(`${t("archiveGroupDetail.title")} - ${groupName}`, token);
    }
  } catch (error) {
    if (!isCurrentToken(token)) {
      return;
    }
    group.value = null;
    items.value = [];
    overview.value = null;
    const message = error instanceof Error ? error.message : t("archiveGroupDetail.notFound");
    notFound.value = /404|not found|未找到/i.test(message);
    errorMessage.value = notFound.value ? t("archiveGroupDetail.notFound") : message;
    await syncTitle(t("archiveGroupDetail.title"), token);
  } finally {
    if (isCurrentToken(token)) {
      loading.value = false;
    }
  }
}

function goBack(): void {
  void router.push({ name: "archive-groups" });
}

function refresh(): void {
  void loadDetail();
}

function viewTask(taskId?: number): void {
  if (!taskId) {
    return;
  }
  void router.push({ name: "task-detail", params: { taskId } });
}

function openEditGroup(): void {
  if (!group.value || busyGroupAction.value) {
    return;
  }
  groupDialogVisible.value = true;
}

async function submitGroup(payload: ArchiveGroupPayload): Promise<void> {
  if (!group.value || groupDialogSubmitting.value) {
    return;
  }
  groupDialogSubmitting.value = true;
  successMessage.value = "";
  try {
    await updateArchiveGroupApi(group.value.id, payload);
    successMessage.value = t("archiveGroup.updated");
    groupDialogVisible.value = false;
    await loadDetail();
  } finally {
    groupDialogSubmitting.value = false;
  }
}

async function toggleGroupStatus(): Promise<void> {
  if (!group.value || busyGroupAction.value || group.value.activeTaskId) {
    return;
  }
  busyGroupAction.value = true;
  successMessage.value = "";
  try {
    await updateArchiveGroupStatusApi(group.value.id, group.value.enableStatus === 0 ? 1 : 0);
    successMessage.value = t("archiveGroup.statusUpdated");
    await loadDetail();
  } finally {
    busyGroupAction.value = false;
  }
}

async function deleteGroup(): Promise<void> {
  if (!group.value || busyGroupAction.value || group.value.activeTaskId) {
    return;
  }
  const confirmed = window.confirm(t("archiveGroup.deleteConfirm"));
  if (!confirmed) {
    return;
  }
  busyGroupAction.value = true;
  successMessage.value = "";
  try {
    await deleteArchiveGroupApi(group.value.id);
    successMessage.value = t("archiveGroup.deleted");
    void router.push({ name: "archive-groups" });
  } finally {
    busyGroupAction.value = false;
  }
}

async function triggerGroup(): Promise<void> {
  if (!group.value || busyGroupAction.value || !canTriggerArchiveGroup(group.value)) {
    return;
  }
  busyGroupAction.value = true;
  successMessage.value = "";
  try {
    const task = await triggerArchiveGroupApi(group.value.id);
    successMessage.value = t("archiveGroup.triggered").replace("{id}", String(task.id));
    await loadDetail();
  } finally {
    busyGroupAction.value = false;
  }
}

function openCreateIdItem(): void {
  idDialogReadonly.value = false;
  activeIdItem.value = null;
  idDialogMode.value = "create";
  idDialogVisible.value = true;
}

function openCreateTimeItem(): void {
  timeDialogReadonly.value = false;
  activeTimeItem.value = null;
  timeDialogMode.value = "create";
  timeDialogVisible.value = true;
}

async function openEditItem(item: ArchiveGroupItemSummary): Promise<void> {
  await openItemDialog(item, false);
}

async function openViewItem(item: ArchiveGroupItemSummary): Promise<void> {
  await openItemDialog(item, true);
}

async function openItemDialog(item: ArchiveGroupItemSummary, readonly: boolean): Promise<void> {
  if (!Number.isFinite(groupId.value) || groupId.value <= 0 || isItemBusy(item.id)) {
    return;
  }
  busyItemIds.value.add(item.id);
  try {
    if (item.itemType === "ID") {
      activeIdItem.value = await getArchiveGroupItemByIdApi(groupId.value, item.id);
      idDialogReadonly.value = readonly;
      idDialogMode.value = "edit";
      idDialogVisible.value = true;
      return;
    }
    activeTimeItem.value = await getArchiveGroupItemByTimeApi(groupId.value, item.id);
    timeDialogReadonly.value = readonly;
    timeDialogMode.value = "edit";
    timeDialogVisible.value = true;
  } finally {
    busyItemIds.value.delete(item.id);
  }
}

async function submitIdItem(payload: ArchiveGroupItemByIdPayload): Promise<void> {
  if (!Number.isFinite(groupId.value) || groupId.value <= 0 || itemDialogSubmitting.value) {
    return;
  }
  itemDialogSubmitting.value = true;
  successMessage.value = "";
  try {
    if (idDialogMode.value === "create") {
      await createArchiveGroupItemByIdApi(groupId.value, payload);
    } else if (activeIdItem.value) {
      await updateArchiveGroupItemByIdApi(groupId.value, activeIdItem.value.id, payload);
    }
    successMessage.value = t("archiveGroup.item.saved");
    resetItemDialogs();
    await loadDetail();
  } finally {
    itemDialogSubmitting.value = false;
  }
}

async function submitTimeItem(payload: ArchiveGroupItemByTimePayload): Promise<void> {
  if (!Number.isFinite(groupId.value) || groupId.value <= 0 || itemDialogSubmitting.value) {
    return;
  }
  itemDialogSubmitting.value = true;
  successMessage.value = "";
  try {
    if (timeDialogMode.value === "create") {
      await createArchiveGroupItemByTimeApi(groupId.value, payload);
    } else if (activeTimeItem.value) {
      await updateArchiveGroupItemByTimeApi(groupId.value, activeTimeItem.value.id, payload);
    }
    successMessage.value = t("archiveGroup.item.saved");
    resetItemDialogs();
    await loadDetail();
  } finally {
    itemDialogSubmitting.value = false;
  }
}

async function toggleItemStatus(item: ArchiveGroupItemSummary): Promise<void> {
  if (!Number.isFinite(groupId.value) || groupId.value <= 0 || isItemBusy(item.id)) {
    return;
  }
  busyItemIds.value.add(item.id);
  successMessage.value = "";
  try {
    const enableStatus = item.enableStatus === 0 ? 1 : 0;
    if (item.itemType === "ID") {
      await updateArchiveGroupItemByIdStatusApi(groupId.value, item.id, enableStatus);
    } else {
      await updateArchiveGroupItemByTimeStatusApi(groupId.value, item.id, enableStatus);
    }
    successMessage.value = t("archiveGroup.item.statusUpdated");
    await loadDetail();
  } finally {
    busyItemIds.value.delete(item.id);
  }
}

async function deleteItem(item: ArchiveGroupItemSummary): Promise<void> {
  if (!Number.isFinite(groupId.value) || groupId.value <= 0 || isItemBusy(item.id)) {
    return;
  }
  const confirmed = window.confirm(t("archiveGroup.item.deleteConfirm"));
  if (!confirmed) {
    return;
  }
  busyItemIds.value.add(item.id);
  successMessage.value = "";
  try {
    if (item.itemType === "ID") {
      await deleteArchiveGroupItemByIdApi(groupId.value, item.id);
    } else {
      await deleteArchiveGroupItemByTimeApi(groupId.value, item.id);
    }
    successMessage.value = t("archiveGroup.item.deleted");
    await loadDetail();
  } finally {
    busyItemIds.value.delete(item.id);
  }
}

watch(
  () => route.params.id,
  () => {
    resetItemDialogs();
    successMessage.value = "";
    void loadDetail();
  },
  { immediate: true }
);
</script>

<template>
  <section class="archive-group-detail-page">
    <header class="page-card archive-group-detail-hero">
      <div>
        <p class="archive-group-detail-hero__eyebrow">{{ t("archiveGroupDetail.title") }}</p>
        <h1>{{ group?.groupName || t("archiveGroupDetail.title") }}</h1>
        <p class="archive-group-detail-hero__meta">
          <span>{{ group?.groupCode || "-" }}</span>
          <span v-if="group?.groupPath">{{ group.groupPath }}</span>
        </p>
      </div>
      <div class="actions">
        <button class="btn btn--subtle" @click="goBack">{{ t("common.back") }}</button>
        <button class="btn btn--subtle" :disabled="loading" @click="refresh">{{ t("common.refresh") }}</button>
        <button class="btn btn--subtle" :disabled="loading || !group || !!group.activeTaskId || busyGroupAction" @click="openEditGroup">
          {{ t("common.edit") }}
        </button>
        <button class="btn btn--subtle" :disabled="loading || !group || !!group.activeTaskId || busyGroupAction" @click="toggleGroupStatus">
          {{ group?.enableStatus === 0 ? t("common.disable") : t("common.enable") }}
        </button>
        <button class="btn btn--subtle" :disabled="loading || !group || !!group.activeTaskId || busyGroupAction" @click="deleteGroup">
          {{ t("common.delete") }}
        </button>
        <button
          v-if="shouldShowArchiveGroupTriggerAction(group)"
          class="btn btn--primary"
          :disabled="loading || !group || busyGroupAction || !canTriggerArchiveGroup(group)"
          @click="triggerGroup"
        >
          {{ t("archiveGroup.trigger") }}
        </button>
        <button v-if="hasArchiveGroupActiveTask(group)" class="btn btn--primary" @click="viewTask(group?.activeTaskId)">
          {{ t("archiveGroupDetail.viewTask") }}
        </button>
      </div>
    </header>

    <p v-if="successMessage" class="feedback">{{ successMessage }}</p>
    <p v-if="errorMessage" :class="notFound ? 'empty' : 'error'">{{ errorMessage }}</p>
    <div v-else-if="loading" class="empty">{{ t("common.loading") }}</div>

    <template v-if="group && overview">
      <section class="archive-group-detail-grid">
        <article class="page-card">
          <h2 class="section-title section-title--top">{{ t("archiveGroup.items") }}</h2>
          <div class="detail-grid">
            <p><strong>{{ t("archiveGroup.columns.code") }}:</strong> {{ group.groupCode }}</p>
            <p><strong>{{ t("archiveGroup.columns.name") }}:</strong> {{ group.groupName }}</p>
            <p><strong>{{ t("archiveGroup.columns.owner") }}:</strong> {{ group.ownerDisplayName || "-" }}</p>
            <p><strong>{{ t("archiveGroup.columns.notifyEnabled") }}:</strong> {{ group.notifyEnabled === 1 ? t("common.yes") : t("common.no") }}</p>
            <p><strong>{{ t("archiveGroup.columns.notifyChannel") }}:</strong> {{ group.notifyEnabled === 1 ? formatNotifyChannel(group.notifyChannel) : "-" }}</p>
            <p>
              <strong>{{ t("archiveGroup.columns.status") }}:</strong>
              <span :class="getStatusTagClass(archiveEnableStatusDictionary, group.enableStatus)">
                {{ getStatusLabel(archiveEnableStatusDictionary, group.enableStatus) }}
              </span>
            </p>
            <p><strong>{{ t("archiveGroup.columns.source") }}:</strong> <EntityLink type="datasource" :id="group.sourceDatasourceId">{{ datasourceName(group.sourceDatasourceId) }}</EntityLink></p>
            <p><strong>{{ t("archiveGroup.columns.target") }}:</strong> <EntityLink type="datasource" :id="group.targetDatasourceId">{{ datasourceName(group.targetDatasourceId) }}</EntityLink></p>
            <p><strong>{{ t("archiveGroup.columns.activeTaskStartTime") }}:</strong> {{ group.activeTaskStartTime || "-" }}</p>
            <div class="archive-group-runtime archive-group-runtime--detail full-width" :class="{ 'archive-group-runtime--idle': !hasArchiveGroupActiveTask(group) }">
              <div class="archive-group-runtime__header">
                <strong>{{ t("archiveGroup.columns.runtimeProgress") }}:</strong>
                <span>{{ resolveArchiveGroupRuntimeProgress(group) }}%</span>
              </div>
              <div class="archive-group-runtime__bar" aria-hidden="true">
                <span
                  class="archive-group-runtime__bar-fill"
                  :style="{ width: `${resolveArchiveGroupRuntimeProgress(group)}%` }"
                />
              </div>
              <p class="archive-group-runtime__summary">
                {{ t("archiveGroup.columns.migratedRecords") }}:
                {{ hasArchiveGroupActiveTask(group) ? getArchiveGroupRuntimeProcessedRecords(group).toLocaleString() : "-" }}
              </p>
            </div>
            <p class="full-width"><strong>{{ t("common.remark") }}:</strong> {{ group.remark || "-" }}</p>
          </div>
        </article>

        <article class="page-card">
          <h2 class="section-title section-title--top">{{ t("archiveGroup.item.title") }}</h2>
          <div class="archive-group-detail-stats">
            <div class="metric-card">
              <p class="metric-card__label">{{ t("archiveGroup.items") }}</p>
              <p class="metric-card__value">{{ overview.itemStats.totalCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("status.enabled") }}</p>
              <p class="metric-card__value">{{ overview.itemStats.enabledCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("status.disabled") }}</p>
              <p class="metric-card__value">{{ overview.itemStats.disabledCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">ID</p>
              <p class="metric-card__value">{{ overview.itemStats.idTypeCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">TIME</p>
              <p class="metric-card__value">{{ overview.itemStats.timeTypeCount }}</p>
            </div>
          </div>
        </article>
      </section>

      <section class="page-card">
        <header class="page-toolbar">
          <div class="section-title-row">
            <h2>{{ t("archiveGroup.item.title") }}</h2>
            <span class="archive-group-detail-count">{{ items.length }}</span>
          </div>
          <div class="actions">
            <button class="btn btn--subtle" :disabled="loading" @click="refresh">{{ t("common.refresh") }}</button>
            <button class="btn btn--subtle" :disabled="loading" @click="openCreateIdItem">
              {{ t("archiveGroup.item.newId") }}
            </button>
            <button class="btn btn--primary" :disabled="loading" @click="openCreateTimeItem">
              {{ t("archiveGroup.item.newTime") }}
            </button>
          </div>
        </header>
        <div v-if="!items.length" class="empty">{{ t("archiveGroup.item.empty") }}</div>
        <div v-else class="table-wrap">
          <table class="table">
            <thead>
              <tr>
                <th>{{ t("archiveGroup.item.type") }}</th>
                <th>{{ t("archiveGroup.item.sourceTable") }}</th>
                <th>{{ t("archiveGroup.item.targetTable") }}</th>
                <th>{{ t("archiveGroup.item.range") }}</th>
                <th>{{ t("archiveGroup.item.priority") }}</th>
                <th>{{ t("archiveGroup.item.stepCount") }}</th>
                <th>{{ t("archiveGroup.item.enableWrite") }}</th>
                <th>{{ t("archiveGroup.item.enableClean") }}</th>
                <th>{{ t("archiveGroup.item.status") }}</th>
                <th>{{ t("common.actions") }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in items" :key="`${item.itemType}:${item.id}`">
                <td>{{ item.itemType }}</td>
                <td>{{ item.sourceTable }}</td>
                <td>{{ item.targetTable }}</td>
                <td>{{ formatArchiveGroupItemRange(item) }}</td>
                <td>{{ item.priority }}</td>
                <td>{{ item.stepCount || "-" }}</td>
                <td>{{ formatSwitchFlag(item.enableWrite) }}</td>
                <td>{{ formatSwitchFlag(item.enableClean) }}</td>
                <td>
                  <span :class="getStatusTagClass(archiveEnableStatusDictionary, item.enableStatus)">
                    {{ getStatusLabel(archiveEnableStatusDictionary, item.enableStatus) }}
                  </span>
                </td>
                <td class="row-actions">
                  <button class="btn btn--subtle" :disabled="isItemBusy(item.id)" @click="openViewItem(item)">
                    {{ t("common.detail") }}
                  </button>
                  <button class="btn btn--subtle" :disabled="isItemBusy(item.id)" @click="openEditItem(item)">
                    {{ t("common.edit") }}
                  </button>
                  <button class="btn btn--subtle" :disabled="isItemBusy(item.id)" @click="toggleItemStatus(item)">
                    {{ item.enableStatus === 0 ? t("common.disable") : t("common.enable") }}
                  </button>
                  <button class="btn btn--subtle" :disabled="isItemBusy(item.id)" @click="deleteItem(item)">
                    {{ t("common.delete") }}
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="archive-group-detail-grid">
        <article class="page-card">
          <h2 class="section-title section-title--top">{{ t("archiveGroupDetail.summary") }}</h2>
          <div class="archive-group-detail-stats">
            <div class="metric-card">
              <p class="metric-card__label">{{ t("task.title") }}</p>
              <p class="metric-card__value">{{ overview.taskStats.totalCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("task.status.success") }}</p>
              <p class="metric-card__value">{{ overview.taskStats.successCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("task.status.failed") }}</p>
              <p class="metric-card__value">{{ overview.taskStats.failedCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("task.status.running") }}</p>
              <p class="metric-card__value">{{ overview.taskStats.runningCount }}</p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("task.columns.status") }}</p>
              <p class="metric-card__value">
                {{ getStatusLabel(taskStatusDictionary, overview.taskStats.lastExecuteStatus) }}
              </p>
            </div>
            <div class="metric-card">
              <p class="metric-card__label">{{ t("task.columns.start") }}</p>
              <p class="metric-card__value archive-group-detail-metric-time">
                {{ formattedLastExecuteTime }}
              </p>
            </div>
          </div>
        </article>

        <article class="page-card">
          <h2 class="section-title section-title--top">{{ t("archiveGroupDetail.recentTasks") }}</h2>
          <div v-if="!overview.recentTasks.length" class="empty">{{ t("archiveGroupDetail.emptyTasks") }}</div>
          <div v-else class="table-wrap">
            <table class="table">
              <thead>
                <tr>
                  <th>{{ t("task.columns.id") }}</th>
                  <th>{{ t("task.columns.status") }}</th>
                  <th>{{ t("task.columns.start") }}</th>
                  <th>{{ t("task.columns.end") }}</th>
                  <th>{{ t("task.columns.processed") }}</th>
                  <th>{{ t("common.actions") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in overview.recentTasks" :key="task.id">
                  <td><EntityLink type="task" :id="task.id">{{ task.id }}</EntityLink></td>
                  <td><TaskStatusTag :status="task.executeStatus" /></td>
                  <td>{{ task.startTime || "-" }}</td>
                  <td>{{ task.endTime || "-" }}</td>
                  <td>{{ task.processedRecords ?? "-" }}</td>
                  <td class="row-actions">
                    <button class="btn btn--subtle" @click="viewTask(task.id)">{{ t("archiveGroupDetail.viewTask") }}</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>
      </section>
    </template>

    <ArchiveGroupItemByIdFormDialog
      :visible="idDialogVisible"
      :mode="idDialogMode"
      :initial-value="activeIdItem"
      :readonly="idDialogReadonly"
      :submitting="itemDialogSubmitting"
      @close="resetItemDialogs"
      @submit="submitIdItem"
    />
    <ArchiveGroupItemByTimeFormDialog
      :visible="timeDialogVisible"
      :mode="timeDialogMode"
      :initial-value="activeTimeItem"
      :readonly="timeDialogReadonly"
      :submitting="itemDialogSubmitting"
      @close="resetItemDialogs"
      @submit="submitTimeItem"
    />
    <ArchiveGroupFormDialog
      :visible="groupDialogVisible"
      mode="edit"
      :initial-value="group"
      :datasources="enabledDatasources"
      :users="users"
      :submitting="groupDialogSubmitting"
      :owner-readonly="!authStore.isAdmin && !authStore.isArchiveAdmin"
      @close="groupDialogVisible = false"
      @submit="submitGroup"
    />
  </section>
</template>

<style scoped>
.archive-group-runtime {
  display: grid;
  gap: 8px;
}

.archive-group-runtime--detail {
  margin-top: 4px;
}

.archive-group-runtime__header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 6px 12px;
}

.archive-group-runtime__bar {
  position: relative;
  overflow: hidden;
  height: 8px;
  border-radius: 999px;
  background: #e6ebf2;
}

.archive-group-runtime__bar-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #1d8f6a 0%, #49b38c 100%);
}

.archive-group-runtime__summary {
  margin: 0;
  color: #526172;
}

.archive-group-runtime--idle .archive-group-runtime__bar-fill {
  background: #c8d2de;
}
</style>
