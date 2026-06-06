<script setup lang="ts">
import {
  getInAppNotificationsApi,
  getInAppNotificationUnreadCountApi,
  markAllInAppNotificationsReadApi,
  markInAppNotificationReadApi,
  type InAppNotificationListItem
} from "../api/inAppNotification";
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { useRoute, useRouter } from "vue-router";

const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const panelOpen = ref(false);
const loading = ref(false);
const loadingUnreadCount = ref(false);
const actionLoading = ref(false);
const errorMessage = ref("");
const unreadCount = ref(0);
const notifications = ref<InAppNotificationListItem[]>([]);
const rootRef = ref<HTMLElement | null>(null);
let refreshTimer: number | null = null;

const hasUnread = computed(() => unreadCount.value > 0);
const unreadBadgeText = computed(() => {
  if (unreadCount.value <= 0) {
    return "";
  }
  return unreadCount.value > 9 ? "9+" : String(unreadCount.value);
});

watch(
  () => route.fullPath,
  () => {
    panelOpen.value = false;
  }
);

onMounted(() => {
  void loadUnreadCount();
  document.addEventListener("mousedown", handleDocumentMouseDown);
  refreshTimer = window.setInterval(() => {
    void loadUnreadCount();
  }, 30000);
});

onBeforeUnmount(() => {
  document.removeEventListener("mousedown", handleDocumentMouseDown);
  if (refreshTimer != null) {
    window.clearInterval(refreshTimer);
  }
});

async function loadUnreadCount(): Promise<void> {
  loadingUnreadCount.value = true;
  try {
    const result = await getInAppNotificationUnreadCountApi();
    unreadCount.value = result.unreadCount;
  } catch {
    // Keep topbar fetch failures silent.
  } finally {
    loadingUnreadCount.value = false;
  }
}

async function loadNotifications(): Promise<void> {
  loading.value = true;
  errorMessage.value = "";
  try {
    notifications.value = await getInAppNotificationsApi(20);
    await loadUnreadCount();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("layout.notifications.loadFailed");
  } finally {
    loading.value = false;
  }
}

function togglePanel(): void {
  panelOpen.value = !panelOpen.value;
  if (panelOpen.value) {
    void loadNotifications();
  }
}

function handleDocumentMouseDown(event: MouseEvent): void {
  if (!panelOpen.value || !rootRef.value) {
    return;
  }
  const target = event.target;
  if (target instanceof Node && !rootRef.value.contains(target)) {
    panelOpen.value = false;
  }
}

function formatTimestamp(value?: string): string {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

async function navigateToNotification(item: InAppNotificationListItem, target: "task" | "group" = "task"): Promise<void> {
  if (actionLoading.value) {
    return;
  }
  actionLoading.value = true;
  try {
    if (item.readStatus === 0) {
      await markInAppNotificationReadApi(item.notificationId);
      item.readStatus = 1;
      unreadCount.value = Math.max(0, unreadCount.value - 1);
    }
    panelOpen.value = false;
    if (target === "task" && item.taskId) {
      await router.push({ name: "task-detail", params: { taskId: item.taskId } });
      return;
    }
    if (item.groupId) {
      await router.push({
        name: "archive-group-detail",
        params: { id: item.groupId },
        query: { title: item.groupName || t("archiveGroupDetail.title") }
      });
    }
  } finally {
    actionLoading.value = false;
  }
}

async function markAllRead(): Promise<void> {
  if (actionLoading.value || unreadCount.value <= 0) {
    return;
  }
  actionLoading.value = true;
  try {
    await markAllInAppNotificationsReadApi();
    unreadCount.value = 0;
    notifications.value = notifications.value.map((item) => ({ ...item, readStatus: 1 }));
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("layout.notifications.loadFailed");
  } finally {
    actionLoading.value = false;
  }
}
</script>

<template>
  <div ref="rootRef" class="notification-bell">
    <button
      type="button"
      class="notification-bell__trigger"
      :aria-label="t('layout.notifications.title')"
      :title="t('layout.notifications.title')"
      @click="togglePanel"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path
          d="M12 3a4 4 0 0 0-4 4v1.2c0 .8-.25 1.59-.72 2.24L5.6 12.8a2 2 0 0 0 1.62 3.2h9.56a2 2 0 0 0 1.62-3.2l-1.68-2.36A3.9 3.9 0 0 1 16 8.2V7a4 4 0 0 0-4-4Zm0 18a2.5 2.5 0 0 0 2.45-2h-4.9A2.5 2.5 0 0 0 12 21Z"
          fill="currentColor"
        />
      </svg>
      <span v-if="hasUnread" class="notification-bell__badge">{{ unreadBadgeText }}</span>
      <span v-else-if="loadingUnreadCount" class="notification-bell__dot" />
    </button>

    <section v-if="panelOpen" class="notification-panel">
      <header class="notification-panel__header">
        <strong>{{ t("layout.notifications.title") }}</strong>
        <button
          type="button"
          class="notification-panel__action"
          :disabled="actionLoading || unreadCount <= 0"
          @click="markAllRead"
        >
          {{ t("layout.notifications.markAllRead") }}
        </button>
      </header>

      <div v-if="loading" class="notification-panel__state">
        {{ t("common.loading") }}
      </div>
      <div v-else-if="errorMessage" class="notification-panel__state notification-panel__state--error">
        {{ errorMessage }}
      </div>
      <div v-else-if="!notifications.length" class="notification-panel__state">
        {{ t("layout.notifications.empty") }}
      </div>
      <ul v-else class="notification-list">
        <li v-for="item in notifications" :key="item.notificationId" class="notification-list__item">
          <button type="button" class="notification-list__card" @click="navigateToNotification(item)">
            <span v-if="item.readStatus === 0" class="notification-list__unread" />
            <div class="notification-list__content">
              <strong>{{ item.title }}</strong>
              <span class="notification-list__time">{{ formatTimestamp(item.createdTime) }}</span>
              <p>{{ item.summary || "-" }}</p>
            </div>
          </button>
          <div class="notification-list__actions">
            <button
              v-if="item.groupId"
              type="button"
              class="notification-panel__action"
              @click.stop="navigateToNotification(item, 'group')"
            >
              {{ t("layout.notifications.viewGroup") }}
            </button>
            <button
              v-if="item.taskId"
              type="button"
              class="notification-panel__action"
              @click.stop="navigateToNotification(item, 'task')"
            >
              {{ t("layout.notifications.viewTask") }}
            </button>
          </div>
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.notification-bell {
  position: relative;
}

.notification-bell__trigger {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(111, 123, 140, 0.24);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  color: #304254;
  cursor: pointer;
}

.notification-bell__trigger svg {
  width: 18px;
  height: 18px;
}

.notification-bell__badge,
.notification-bell__dot {
  position: absolute;
  top: 4px;
  right: 2px;
}

.notification-bell__badge {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #d94a3a;
  color: #fff;
  font-size: 11px;
  line-height: 18px;
}

.notification-bell__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #d94a3a;
}

.notification-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 0;
  z-index: 20;
  width: min(360px, calc(100vw - 24px));
  max-height: 480px;
  overflow: hidden auto;
  border: 1px solid rgba(111, 123, 140, 0.24);
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 20px 50px rgba(32, 54, 81, 0.16);
}

.notification-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #ebeff4;
}

.notification-panel__action {
  padding: 0;
  border: 0;
  background: transparent;
  color: #1f7a59;
  font-size: 12px;
  cursor: pointer;
}

.notification-panel__action:disabled {
  color: #9aa7b3;
  cursor: not-allowed;
}

.notification-panel__state {
  padding: 18px 16px;
  color: #526172;
  font-size: 13px;
}

.notification-panel__state--error {
  color: #b43a2d;
}

.notification-list {
  margin: 0;
  padding: 8px;
  list-style: none;
}

.notification-list__item {
  display: grid;
  gap: 6px;
}

.notification-list__card {
  position: relative;
  width: 100%;
  padding: 12px 12px 12px 18px;
  border: 0;
  border-radius: 14px;
  background: #f6f8fb;
  text-align: left;
  cursor: pointer;
}

.notification-list__card:hover {
  background: #eef4f1;
}

.notification-list__unread {
  position: absolute;
  top: 14px;
  left: 8px;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #d94a3a;
}

.notification-list__content {
  display: grid;
  gap: 4px;
}

.notification-list__content strong {
  color: #203651;
  font-size: 13px;
}

.notification-list__time,
.notification-list__content p {
  margin: 0;
  color: #5a6a7b;
  font-size: 12px;
  line-height: 1.45;
}

.notification-list__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 12px 8px;
}
</style>
