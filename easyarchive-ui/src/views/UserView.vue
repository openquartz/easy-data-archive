<script setup lang="ts">
import {
  createUserApi,
  getUserDatasourcePermissionsApi,
  getUsersApi,
  replaceUserDatasourcePermissionsApi,
  type User,
  type UserDatasourcePermissionItem,
  type UserPayload,
  updateUserApi,
  updateUserStatusApi
} from "../api/user";
import { getDatasourcesApi, type Datasource } from "../api/datasource";
import UserFormDialog from "../components/UserFormDialog.vue";
import UserPermissionDialog from "../components/UserPermissionDialog.vue";
import { getStatusLabel, getStatusTagClass, userStatusDictionary } from "../utils/dictionaries";
import { computed, ref } from "vue";
import { useI18n } from "../i18n";
import { useAuthStore } from "../stores/auth";
import { isAdminRole, normalizeRoleCode } from "../constants/roles";

const loading = ref(false);
const list = ref<User[]>([]);
const datasources = ref<Datasource[]>([]);
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
const busyRows = ref(new Set<number>());
const busyActions = ref(new Set<string>());

const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogSubmitting = ref(false);
const activeItem = ref<User | null>(null);
const permissionDialogVisible = ref(false);
const permissionDialogLoading = ref(false);
const permissionDialogSubmitting = ref(false);
const permissionSelectedIds = ref<number[]>([]);
const { t } = useI18n();
const authStore = useAuthStore();
const isAdmin = computed(() => Boolean(authStore.profile?.isAdmin));

const emptyText = computed(() => (loading.value ? t("user.emptyLoading") : t("user.empty")));
const getActionKey = (action: string, id: number): string => `${action}:${id}`;
const isRowBusy = (id: number): boolean => busyRows.value.has(id);
const isActionBusy = (action: string, id: number): boolean => busyActions.value.has(getActionKey(action, id));

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    const [userList, datasourceList] = await Promise.all([getUsersApi(), getDatasourcesApi()]);
    list.value = userList;
    datasources.value = datasourceList;
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("user.loadFailed");
  } finally {
    loading.value = false;
  }
}

function openCreate(): void {
  dialogMode.value = "create";
  activeItem.value = null;
  dialogVisible.value = true;
}

function openEdit(item: User): void {
  dialogMode.value = "edit";
  activeItem.value = item;
  dialogVisible.value = true;
}

async function openPermissions(item: User): Promise<void> {
  if (!isAdmin.value || isRowBusy(item.id)) {
    return;
  }
  activeItem.value = item;
  permissionDialogVisible.value = true;
  permissionDialogLoading.value = true;
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    const result = await getUserDatasourcePermissionsApi(item.id);
    permissionSelectedIds.value = result.map((permission: UserDatasourcePermissionItem) => permission.id);
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("user.permissions.loadFailed");
  } finally {
    permissionDialogLoading.value = false;
  }
}

async function submitForm(payload: UserPayload): Promise<void> {
  if (dialogSubmitting.value) {
    return;
  }
  dialogSubmitting.value = true;
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    if (dialogMode.value === "create") {
      await createUserApi(payload);
      successMessage.value = t("user.created");
    } else if (activeItem.value) {
      await updateUserApi(activeItem.value.id, payload);
      successMessage.value = t("user.updated");
    }
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("user.saveFailed");
  } finally {
    dialogSubmitting.value = false;
  }
}

async function submitPermissions(datasourceIds: number[]): Promise<void> {
  if (!activeItem.value || permissionDialogSubmitting.value) {
    return;
  }
  permissionDialogSubmitting.value = true;
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    await replaceUserDatasourcePermissionsApi(activeItem.value.id, { datasourceIds });
    permissionSelectedIds.value = datasourceIds;
    successMessage.value = t("user.permissions.updated");
    permissionDialogVisible.value = false;
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("user.permissions.updateFailed");
  } finally {
    permissionDialogSubmitting.value = false;
  }
}

async function toggleStatus(item: User): Promise<void> {
  if (isRowBusy(item.id) || isActionBusy("toggleStatus", item.id)) {
    return;
  }
  const nextStatus = item.status === 0 ? 1 : 0;
  const actionKey = getActionKey("toggleStatus", item.id);
  busyRows.value.add(item.id);
  busyActions.value.add(actionKey);
  successMessage.value = "";
  actionErrorMessage.value = "";
  try {
    await updateUserStatusApi(item.id, nextStatus);
    successMessage.value = t("user.statusUpdated");
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : t("user.statusUpdateFailed");
  } finally {
    busyActions.value.delete(actionKey);
    busyRows.value.delete(item.id);
  }
}

void loadData();
</script>

<template>
  <section class="page-card">
    <div v-if="!isAdmin" class="empty">{{ t("user.noAccess") }}</div>
    <template v-else>
    <header class="page-toolbar">
      <h1>{{ t("user.title") }}</h1>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">{{ t("common.refresh") }}</button>
        <button class="btn btn--primary" :disabled="loading" @click="openCreate">{{ t("user.new") }}</button>
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
            <th>{{ t("user.columns.username") }}</th>
            <th>{{ t("user.columns.realName") }}</th>
            <th>{{ t("user.columns.email") }}</th>
            <th>{{ t("user.columns.mobile") }}</th>
            <th>{{ t("user.columns.roleCode") }}</th>
            <th>{{ t("user.columns.status") }}</th>
            <th>{{ t("user.columns.lastLogin") }}</th>
            <th>{{ t("user.columns.actions") }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.username }}</td>
            <td>{{ item.realName || "-" }}</td>
            <td>{{ item.email || "-" }}</td>
            <td>{{ item.mobile || "-" }}</td>
            <td>{{ t(`user.roles.${normalizeRoleCode(item.roleCode)}`) }}</td>
            <td><span :class="getStatusTagClass(userStatusDictionary, item.status)">{{ getStatusLabel(userStatusDictionary, item.status) }}</span></td>
            <td>{{ item.lastLoginTime || "-" }}</td>
            <td class="row-actions">
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="openEdit(item)">{{ t("common.edit") }}</button>
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="openPermissions(item)">
                {{ t("user.permissions.action") }}
              </button>
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="toggleStatus(item)">
                {{ item.status === 0 ? t("common.disable") : t("common.enable") }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <UserFormDialog
      :visible="dialogVisible"
      :mode="dialogMode"
      :initial-value="activeItem"
      :submitting="dialogSubmitting"
      @close="dialogVisible = false"
      @submit="submitForm"
    />
    <UserPermissionDialog
      :visible="permissionDialogVisible"
      :loading="permissionDialogLoading"
      :submitting="permissionDialogSubmitting"
      :username="activeItem?.username"
      :datasources="datasources"
      :selected-ids="permissionSelectedIds"
      :is-admin-user="isAdminRole(activeItem?.roleCode)"
      @close="permissionDialogVisible = false"
      @submit="submitPermissions"
    />
    </template>
  </section>
</template>
