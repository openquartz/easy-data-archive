<script setup lang="ts">
import LanguageSwitcher from "../components/LanguageSwitcher.vue";
import { computed, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { useWorkTabsStore, getTabKey } from "../stores/workTabs";

const { t } = useI18n();
const authStore = useAuthStore();
const router = useRouter();
const route = useRoute();
const loggingOut = ref(false);
const workTabsStore = useWorkTabsStore();

const accountLabel = computed(
  () => authStore.profile?.realName || authStore.username || authStore.profile?.username || ""
);

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
  loggingOut.value = true;
  try {
    await authStore.logout();
    await router.push({ name: "login" });
  } finally {
    loggingOut.value = false;
  }
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
      <div class="brand">{{ t("layout.brand") }}</div>
      <nav class="nav">
        <RouterLink class="nav__item" :to="{ name: 'dashboard' }">{{ t("layout.nav.dashboard") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'datasources' }">{{ t("layout.nav.datasources") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'archive-groups' }">{{ t("layout.nav.archiveGroups") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'tasks' }">{{ t("layout.nav.tasks") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'guide' }">{{ t("layout.nav.guide") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'users' }">{{ t("layout.nav.users") }}</RouterLink>
      </nav>
    </aside>
    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <strong>{{ t("layout.topbar") }}</strong>
        <div class="app-shell__topbar-actions">
          <span v-if="accountLabel" class="account-pill">{{ accountLabel }}</span>
          <button class="btn btn--subtle" :disabled="loggingOut" @click="handleLogout">
            {{ loggingOut ? t("layout.actions.loggingOut") : t("layout.actions.logout") }}
          </button>
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
</template>
