<script setup lang="ts">
import {
  createUserApi,
  getUsersApi,
  type User,
  type UserPayload,
  updateUserApi,
  updateUserStatusApi
} from "../api/user";
import UserFormDialog from "../components/UserFormDialog.vue";
import { getStatusLabel, getStatusTagClass, userStatusDictionary } from "../utils/dictionaries";
import { computed, ref } from "vue";

const loading = ref(false);
const list = ref<User[]>([]);
const errorMessage = ref("");
const successMessage = ref("");
const actionErrorMessage = ref("");
const busyRows = ref(new Set<number>());
const busyActions = ref(new Set<string>());

const dialogVisible = ref(false);
const dialogMode = ref<"create" | "edit">("create");
const dialogSubmitting = ref(false);
const activeItem = ref<User | null>(null);

const emptyText = computed(() => (loading.value ? "Loading users..." : "No user records."));
const getActionKey = (action: string, id: number): string => `${action}:${id}`;
const isRowBusy = (id: number): boolean => busyRows.value.has(id);
const isActionBusy = (action: string, id: number): boolean => busyActions.value.has(getActionKey(action, id));

async function loadData(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    list.value = await getUsersApi();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : "Failed to load users";
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
      successMessage.value = "User created.";
    } else if (activeItem.value) {
      await updateUserApi(activeItem.value.id, payload);
      successMessage.value = "User updated.";
    }
    dialogVisible.value = false;
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : "Save failed";
  } finally {
    dialogSubmitting.value = false;
  }
}

async function toggleStatus(item: User): Promise<void> {
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
    await updateUserStatusApi(item.id, nextStatus);
    successMessage.value = "User status updated.";
    await loadData();
  } catch (error) {
    actionErrorMessage.value = error instanceof Error ? error.message : "Status update failed";
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
      <h1>User Management</h1>
      <div class="actions">
        <button class="btn btn--subtle" :disabled="loading" @click="loadData">Refresh</button>
        <button class="btn btn--primary" :disabled="loading" @click="openCreate">New User</button>
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
            <th>Username</th>
            <th>Real Name</th>
            <th>Email</th>
            <th>Mobile</th>
            <th>Status</th>
            <th>Last Login</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in list" :key="item.id">
            <td>{{ item.username }}</td>
            <td>{{ item.realName || "-" }}</td>
            <td>{{ item.email || "-" }}</td>
            <td>{{ item.mobile || "-" }}</td>
            <td><span :class="getStatusTagClass(userStatusDictionary, item.status)">{{ getStatusLabel(userStatusDictionary, item.status) }}</span></td>
            <td>{{ item.lastLoginTime || "-" }}</td>
            <td class="row-actions">
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="openEdit(item)">Edit</button>
              <button class="btn btn--subtle" :disabled="isRowBusy(item.id)" @click="toggleStatus(item)">
                {{ item.status === 1 ? "Disable" : "Enable" }}
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
  </section>
</template>
