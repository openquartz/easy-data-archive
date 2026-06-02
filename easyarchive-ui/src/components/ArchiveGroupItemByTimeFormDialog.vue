<script setup lang="ts">
import type { ArchiveGroupItemByTime, ArchiveGroupItemByTimePayload } from "../api/archiveGroupItem";
import { computed, reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";

const props = defineProps<{
  visible: boolean;
  mode: "create" | "edit";
  initialValue?: ArchiveGroupItemByTime | null;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "submit", payload: ArchiveGroupItemByTimePayload): void;
}>();

const form = reactive<ArchiveGroupItemByTimePayload>({
  sourceTable: "",
  targetTable: "",
  priority: 10,
  fetchSql: "",
  deleteWhere: "",
  startTime: "",
  keepDay: 30,
  stepMinutes: 60,
  stepCount: 1000,
  pauseMs: undefined,
  enableClean: 0,
  enableWrite: 0,
  enableStatus: 0,
  idColumn: "ID"
});
const errorMessage = ref("");
const { t } = useI18n();
const title = computed(() =>
  props.mode === "create" ? t("archiveGroup.item.timeCreateTitle") : t("archiveGroup.item.timeEditTitle")
);

watch(
  () => [props.visible, props.initialValue, props.mode],
  () => {
    errorMessage.value = "";
    Object.assign(form, {
      sourceTable: props.initialValue?.sourceTable || "",
      targetTable: props.initialValue?.targetTable || "",
      priority: props.initialValue?.priority ?? 10,
      fetchSql: props.initialValue?.fetchSql || "",
      deleteWhere: props.initialValue?.deleteWhere || "",
      startTime: props.initialValue?.startTime?.slice(0, 16) || "",
      keepDay: props.initialValue?.keepDay ?? 30,
      stepMinutes: props.initialValue?.stepMinutes ?? 60,
      stepCount: props.initialValue?.stepCount ?? 1000,
      pauseMs: props.initialValue?.pauseMs,
      enableClean: props.initialValue?.enableClean ?? 0,
      enableWrite: props.initialValue?.enableWrite ?? 0,
      enableStatus: props.initialValue?.enableStatus ?? 0,
      idColumn: props.initialValue?.idColumn || "ID"
    });
  },
  { immediate: true }
);

function validate(): boolean {
  if (!form.sourceTable.trim() || !form.targetTable.trim() || !form.idColumn.trim() || !form.startTime) {
    errorMessage.value = t("archiveGroup.item.validation.tableRequired");
    return false;
  }
  if (!form.fetchSql.trim()) {
    errorMessage.value = t("archiveGroup.item.validation.sqlRequired");
    return false;
  }
  if (form.priority <= 0 || form.stepCount <= 0 || form.stepMinutes <= 0 || form.keepDay < 0) {
    errorMessage.value = t("archiveGroup.item.validation.positiveRequired");
    return false;
  }
  return true;
}

function handleSubmit(): void {
  if (props.submitting || !validate()) {
    return;
  }
  emit("submit", {
    ...form,
    sourceTable: form.sourceTable.trim(),
    targetTable: form.targetTable.trim(),
    fetchSql: form.fetchSql.trim(),
    deleteWhere: form.deleteWhere?.trim(),
    idColumn: form.idColumn.trim()
  });
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card">
      <header class="modal-card__header"><h3>{{ title }}</h3></header>
      <form class="form-grid" @submit.prevent="handleSubmit">
        <label>{{ t("archiveGroup.item.sourceTable") }}<input v-model="form.sourceTable" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.targetTable") }}<input v-model="form.targetTable" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.priority") }}<input v-model.number="form.priority" type="number" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.idColumn") }}<input v-model="form.idColumn" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.startTime") }}<input v-model="form.startTime" type="datetime-local" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.keepDay") }}<input v-model.number="form.keepDay" type="number" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.stepMinutes") }}<input v-model.number="form.stepMinutes" type="number" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.stepCount") }}<input v-model.number="form.stepCount" type="number" :disabled="submitting" /></label>
        <label>{{ t("archiveGroup.item.pauseMs") }}<input v-model.number="form.pauseMs" type="number" :disabled="submitting" /></label>
        <label>
          {{ t("archiveGroup.item.enableWrite") }}
          <select v-model.number="form.enableWrite" :disabled="submitting">
            <option :value="0">{{ t("common.yes") }}</option>
            <option :value="1">{{ t("common.no") }}</option>
          </select>
        </label>
        <label>
          {{ t("archiveGroup.item.enableClean") }}
          <select v-model.number="form.enableClean" :disabled="submitting">
            <option :value="0">{{ t("common.yes") }}</option>
            <option :value="1">{{ t("common.no") }}</option>
          </select>
        </label>
        <label>
          {{ t("archiveGroup.item.status") }}
          <select v-model.number="form.enableStatus" :disabled="submitting">
            <option :value="0">{{ t("status.enabled") }}</option>
            <option :value="1">{{ t("status.disabled") }}</option>
          </select>
        </label>
        <label class="full-width">{{ t("archiveGroup.item.fetchSql") }}<textarea v-model="form.fetchSql" :disabled="submitting" /></label>
        <label class="full-width">{{ t("archiveGroup.item.deleteWhere") }}<textarea v-model="form.deleteWhere" :disabled="submitting" /></label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <footer class="modal-card__footer">
          <button type="button" class="btn btn--subtle" :disabled="submitting" @click="emit('close')">{{ t("common.cancel") }}</button>
          <button type="submit" class="btn btn--primary" :disabled="submitting">{{ submitting ? t("common.saving") : t("common.save") }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>
