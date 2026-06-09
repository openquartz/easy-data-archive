<script setup lang="ts">
import AppBrand from "../components/AppBrand.vue";
import ChangePasswordDialog from "../components/ChangePasswordDialog.vue";
import InAppNotificationBell from "../components/InAppNotificationBell.vue";
import LanguageSwitcher from "../components/LanguageSwitcher.vue";
import { computed, ref, watch } from "vue";
import { buildPrimaryNavItems } from "../content/navigation";
import { useI18n } from "../i18n";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { showSuccessToast } from "../stores/toast";
import { useWorkTabsStore, getTabKey } from "../stores/workTabs";

const { t } = useI18n();
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const loggingOut = ref(false);
const userMenuOpen = ref(false);
const changePasswordVisible = ref(false);
const changePasswordSubmitting = ref(false);
const workTabsStore = useWorkTabsStore();

const accountLabel = computed(
  () => authStore.profile?.realName || authStore.username || authStore.profile?.username || ""
);
const primaryNavItems = computed(() => buildPrimaryNavItems(authStore.isAdmin, authStore.hasCapability));

watch(
  () => route.fullPath,
  () => {
    syncWorkTabs();
  },
  { immediate: true }
);

async function handleLogout(): Promise<void> {
  if (loggingOut.value) {
    return;
  }
  userMenuOpen.value = false;
  loggingOut.value = true;
  try {
    await authStore.logout();
    await router.push({ name: "login" });
  } finally {
    loggingOut.value = false;
  }
}

function openChangePassword(): void {
  userMenuOpen.value = false;
  changePasswordVisible.value = true;
}

async function handlePasswordChanged(): Promise<void> {
  changePasswordVisible.value = false;
  showSuccessToast(t("common.passwordChanged"));
  await authStore.logout();
  await router.push({ name: "login" });
}

function resolveParam(value: unknown): string {
  if (Array.isArray(value)) {
    return value[0] ? String(value[0]) : "";
  }
  return value == null ? "" : String(value);
}

function resolveQueryTitle(): string {
  const { title } = route.query;
  if (Array.isArray(title)) {
    return title[0] || t("workTab.groupDetail");
  }
  if (typeof title === "string" && title.trim()) {
    return title;
  }
  return t("workTab.groupDetail");
}

function syncWorkTabs(): void {
  if (route.name === "archive-group-detail") {
    const id = resolveParam(route.params.id);
    if (!id) {
      return;
    }
    workTabsStore.openTab({
      type: "group-detail",
      id,
      title: resolveQueryTitle()
    });
  } else if (route.name === "task-detail") {
    const taskId = resolveParam(route.params.taskId);
    if (!taskId) {
      return;
    }
    workTabsStore.openTab({
      type: "task-detail",
      taskId,
      title: `${t("workTab.task")} #${taskId}`
    });
  }
}

function openWorkTab(tab: { type: string; id?: string; taskId?: string; title?: string }): void {
  if (tab.type === "group-detail" && tab.id) {
    void router.push({
      name: "archive-group-detail",
      params: { id: tab.id },
      query: { title: tab.title }
    });
  } else if (tab.type === "task-detail" && tab.taskId) {
    void router.push({
      name: "task-detail",
      params: { taskId: tab.taskId }
    });
  }
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <AppBrand :title="t('layout.brand')" :subtitle="t('layout.topbar')" />
      <nav class="nav">
        <RouterLink v-for="item in primaryNavItems" :key="item.routeName" class="nav__item" :to="{ name: item.routeName }">
          {{ t(item.labelKey) }}
        </RouterLink>
      </nav>
    </aside>
    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <strong>{{ t("layout.topbar") }}</strong>
        <div class="app-shell__topbar-actions">
          <div class="user-menu" :class="{ 'user-menu--open': userMenuOpen }">
            <button
              type="button"
              class="account-pill user-menu__trigger"
              @click="userMenuOpen = !userMenuOpen"
            >
              {{ accountLabel }} ▾
            </button>
            <div v-if="userMenuOpen" class="user-menu__dropdown" @click.stop>
              <button type="button" class="user-menu__item" @click="openChangePassword">
                {{ t("layout.actions.changePassword") }}
              </button>
              <button
                type="button"
                class="user-menu__item"
                :disabled="loggingOut"
                @click="handleLogout"
              >
                {{ loggingOut ? t("layout.actions.loggingOut") : t("layout.actions.logout") }}
              </button>
            </div>
          </div>
          <InAppNotificationBell />
          <LanguageSwitcher />
        </div>
      </header>
      <main class="app-shell__content">
        <div v-if="workTabsStore.hasTabs" class="work-tabs" aria-label="workspace tabs">
          <div
            v-for="tab in workTabsStore.tabs"
            :key="getTabKey(tab)"
            class="work-tabs__item"
            :class="{ 'work-tabs__item--active': getTabKey(tab) === workTabsStore.activeKey }"
          >
            <button type="button" class="work-tabs__trigger" @click="openWorkTab(tab)">{{ tab.title }}</button>
            <button type="button" class="work-tabs__close" aria-label="Close tab" @click="workTabsStore.closeTab(getTabKey(tab))">×</button>
          </div>
        </div>
        <router-view />
      </main>
    </div>
  </div>
  <ChangePasswordDialog
    :visible="changePasswordVisible"
    :submitting="changePasswordSubmitting"
    @close="changePasswordVisible = false"
    @password-changed="handlePasswordChanged"
  />
</template>

<style scoped>
.user-menu {
  position: relative;
  display: inline-block;
}

.user-menu__trigger {
  cursor: pointer;
  font-weight: 600;
}

.user-menu--open .user-menu__trigger {
  color: #164b5a;
}

.user-menu__dropdown {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  min-width: 160px;
  background: #fff;
  border: 1px solid rgba(16, 57, 71, 0.12);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
  padding: 6px;
  z-index: 100;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-menu__item {
  width: 100%;
  text-align: left;
  padding: 8px 12px;
  border: 0;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  font-size: 0.9rem;
  color: #1e293b;
  transition: background 120ms ease;
}

.user-menu__item:hover {
  background: rgba(16, 57, 71, 0.06);
}

.user-menu__item:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
