import { computed, ref } from "vue";
import { defineStore } from "pinia";
import { useRouter } from "vue-router";

export interface WorkTabGroupDetail {
  type: "group-detail";
  id: string;
  title: string;
}

export interface WorkTabTaskDetail {
  type: "task-detail";
  taskId: string;
  title: string;
}

export type WorkTab = WorkTabGroupDetail | WorkTabTaskDetail;

export function getTabKey(tab: WorkTab): string {
  if (tab.type === "group-detail") {
    return `group-detail:${tab.id}`;
  }
  return `task-detail:${tab.taskId}`;
}

export const useWorkTabsStore = defineStore("workTabs", () => {
  const router = useRouter();
  const tabs = ref<WorkTab[]>([]);
  const activeKey = ref<string | null>(null);

  const hasTabs = computed(() => tabs.value.length > 0);

  function openTab(tab: WorkTab): void {
    const key = getTabKey(tab);
    activeKey.value = key;
    const existingIndex = tabs.value.findIndex((t) => getTabKey(t) === key);
    if (existingIndex >= 0) {
      tabs.value[existingIndex] = tab;
      return;
    }
    tabs.value = [...tabs.value, tab];
  }

  function closeTab(key: string): void {
    const currentIndex = tabs.value.findIndex((t) => getTabKey(t) === key);
    if (currentIndex < 0) {
      return;
    }
    const closingTab = tabs.value[currentIndex];
    const closingActive = activeKey.value === key;
    const remaining = tabs.value.filter((t) => getTabKey(t) !== key);
    tabs.value = remaining;
    if (!closingActive) {
      return;
    }
    const fallback = remaining[currentIndex] || remaining[currentIndex - 1];
    if (fallback) {
      activeKey.value = getTabKey(fallback);
      void router.push(tabToRoute(fallback));
      return;
    }
    activeKey.value = null;
    const fallbackRoute = closingTab.type === "task-detail"
      ? { name: "tasks" as const }
      : { name: "archive-groups" as const };
    void router.push(fallbackRoute);
  }

  function closeAll(): void {
    tabs.value = [];
    activeKey.value = null;
  }

  function tabToRoute(tab: WorkTab) {
    if (tab.type === "group-detail") {
      return {
        name: "archive-group-detail" as const,
        params: { id: tab.id },
        query: { title: tab.title }
      };
    }
    return {
      name: "task-detail" as const,
      params: { taskId: tab.taskId }
    };
  }

  return {
    tabs,
    activeKey,
    hasTabs,
    openTab,
    closeTab,
    closeAll,
    tabToRoute
  };
});
