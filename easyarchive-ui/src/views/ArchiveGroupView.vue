<script setup lang="ts">
import {
  cancelArchiveGroupActiveTaskApi,
  createArchiveGroupApi,
  deleteArchiveGroupApi,
  type ArchiveGroup,
  type ArchiveGroupPayload,
  getArchiveGroupsApi,
  triggerArchiveGroupApi,
  updateArchiveGroupApi,
  updateArchiveGroupStatusApi
} from "../api/archiveGroup";
import { getDatasourcesApi, type Datasource } from "../api/datasource";
import ArchiveGroupFormDialog from "../components/ArchiveGroupFormDialog.vue";
import TaskStatusTag from "../components/TaskStatusTag.vue";
import EntityLink from "../components/EntityLink.vue";
import { archiveEnableStatusDictionary, getStatusLabel, getStatusTagClass } from "../utils/dictionaries";
import { computed, onBeforeUnmount, ref } from "vue";
import { useI18n } from "../i18n";
import { useRouter } from "vue-router";
import { createPolling } from "../utils/polling";

const loading = ref(false);
const groups = ref<ArchiveGroup[]>([]);
const datasources = ref<Datasource[]>([]);
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
const busyRows = ref(new Set<number>());
const busyActions = ref(new Set<string>());

const groupDialogVisible = ref(false);
const groupDialogMode = ref<"create" | "edit">("create");
const groupDialogSubmitting = ref(false);
const activeGroup = ref<ArchiveGroup | null>(null);
const { t } = useI18n();
const router = useRouter();

const groupEmptyText = computed(() => (loading.value ? t("archiveGroup.emptyLoading") : t("archiveGroup.empty")));
const hasActiveTasks = computed(() => groups.value.some((item) => typeof item.activeTaskId === "number"));

const getActionKey = (action: string, id: number): string => `${action}:${id}`;
const isRowBusy = (id: number): boolean => busyRows.value.has(id);
const isActionBusy = (action: string, id: number): boolean => busyActions.value.has(getActionKey(action, id));
const datasourceName = (id: number): string => datasources.value.find((item) => item.id === id)?.datasourceName || String(id);
const datasourceCode = (id: number): string => datasources.value.find((item) => item.id === id)?.datasourceCode || "";

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [groupResult, datasourceResult] = await Promise.all([getArchiveGroupsApi(), getDatasourcesApi()]);
    groups.value = groupResult;
    datasources.value = datasourceResult;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("archiveGroup.loadFailed");
  } finally {
    loading.value = false;
    syncPolling();
  }
}

function openDetail(group: ArchiveGroup): void {
  void router.push({
    name: "archive-group-detail",
    params: { id: group.id },
    query: { title: `${t("archiveGroupDetail.title")} - ${group.groupName}` }
  });
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
      await createArchiveGroupApi(payload);
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

async function cancelGroupTask(group: ArchiveGroup): Promise<void> {
  if (!group.activeTaskId) {
    return;
  }
  const confirmed = window.confirm(t("task.cancelConfirm", { id: group.activeTaskId }));
  if (!confirmed) {
    return;
  }
  await runGroupAction("cancelTask", group.id, async () => {
    const task = await cancelArchiveGroupActiveTaskApi(group.id);
    successMessage.value = t("archiveGroup.cancelSubmitted").replace("{id}", String(task.id));
    await loadData();
  });
}

function viewTask(group: ArchiveGroup): void {
  if (!group.activeTaskId) {
    return;
  }
  void router.push({ name: "task-detail", params: { taskId: group.activeTaskId } });
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

const poller = createPolling(loadData, { intervalMs: 5000, immediate: false });

function syncPolling(): void {
  if (hasActiveTasks.value) {
    poller.start();
    return;
  }
  poller.stop();
}

void loadData();

onBeforeUnmount(() => {
  poller.stop();
});
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
            <th>{{ t("archiveGroup.columns.activeTask") }}</th>
            <th>{{ t("archiveGroup.columns.activeTaskStartTime") }}</th>
            <th>{{ t("archiveGroup.columns.actions") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="group in groups" :key="group.id">
            <td><EntityLink type="group" :id="group.id" :title="group.groupName">{{ group.groupCode }}</EntityLink></td>
            <td>{{ group.groupName }}</td>
            <td><EntityLink type="datasource" :id="group.sourceDatasourceId">{{ datasourceName(group.sourceDatasourceId) }}</EntityLink></td>
            <td><EntityLink type="datasource" :id="group.targetDatasourceId">{{ datasourceName(group.targetDatasourceId) }}</EntityLink></td>
            <td>
              <span :class="getStatusTagClass(archiveEnableStatusDictionary, group.enableStatus)">
                {{ getStatusLabel(archiveEnableStatusDictionary, group.enableStatus) }}
              </span>
            </td>
            <td>
              <TaskStatusTag v-if="group.activeTaskId" :status="group.activeTaskStatus" />
              <span v-else>-</span>
            </td>
            <td>{{ group.activeTaskStartTime || "-" }}</td>
            <td class="row-actions">
              <button class="btn btn--subtle" :disabled="isRowBusy(group.id)" @click.stop="openDetail(group)">
                {{ t("archiveGroupDetail.openDetail") }}
              </button>
              <button
                class="btn btn--subtle"
                :disabled="isRowBusy(group.id) || !!group.activeTaskId"
                @click.stop="openEditGroup(group)"
              >
                {{ t("common.edit") }}
              </button>
              <button
                class="btn btn--subtle"
                :disabled="isRowBusy(group.id) || !!group.activeTaskId"
                @click.stop="toggleGroupStatus(group)"
              >
                {{ group.enableStatus === 0 ? t("common.disable") : t("common.enable") }}
              </button>
              <button
                v-if="group.canTrigger !== false"
                class="btn btn--subtle"
                :disabled="isRowBusy(group.id) || !group.canTrigger"
                @click.stop="triggerGroup(group)"
              >
                {{ t("archiveGroup.trigger") }}
              </button>
              <button
                v-if="group.activeTaskId"
                class="btn btn--subtle"
                :disabled="isRowBusy(group.id) || !group.canCancelActiveTask || group.activeTaskStatus === 4"
                @click.stop="cancelGroupTask(group)"
              >
                {{ group.activeTaskStatus === 4 ? t("task.cancelling") : t("task.cancelAction") }}
              </button>
              <button
                v-if="group.activeTaskId"
                class="btn btn--subtle"
                :disabled="isRowBusy(group.id)"
                @click.stop="viewTask(group)"
              >
                {{ t("archiveGroup.viewTask") }}
              </button>
              <button
                class="btn btn--subtle"
                :disabled="isRowBusy(group.id) || !!group.activeTaskId"
                @click.stop="deleteGroup(group)"
              >
                {{ t("common.delete") }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <ArchiveGroupFormDialog
      :visible="groupDialogVisible"
      :mode="groupDialogMode"
      :initial-value="activeGroup"
      :datasources="datasources"
      :submitting="groupDialogSubmitting"
      @close="groupDialogVisible = false"
      @submit="submitGroup"
    />
  </section>
</template>
