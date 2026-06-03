<script setup lang="ts">
import { useRouter } from "vue-router";
import { useI18n } from "../i18n";
import { useWorkTabsStore, type WorkTab } from "../stores/workTabs";

const props = defineProps<{
  type: "group" | "task" | "datasource";
  id?: string | number;
  title?: string;
}>();

const router = useRouter();
const workTabsStore = useWorkTabsStore();
const { t } = useI18n();

function handleClick(event: MouseEvent): void {
  event.preventDefault();
  if (props.type === "group" && props.id != null) {
    const tab: WorkTab = {
      type: "group-detail",
      id: String(props.id),
      title: props.title || String(props.id)
    };
    workTabsStore.openTab(tab);
    void router.push(workTabsStore.tabToRoute(tab));
  } else if (props.type === "task" && props.id != null) {
    const tab: WorkTab = {
      type: "task-detail",
      taskId: String(props.id),
      title: `${t("workTab.task")} #${props.id}`
    };
    workTabsStore.openTab(tab);
    void router.push(workTabsStore.tabToRoute(tab));
  } else if (props.type === "datasource") {
    void router.push({ name: "datasources" });
  }
}
</script>

<template>
  <a class="entity-link" href="#" @click="handleClick"><slot /></a>
</template>
