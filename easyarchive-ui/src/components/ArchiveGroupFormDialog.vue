<script setup lang="ts">
import type { ArchiveGroup, ArchiveGroupPayload } from "../api/archiveGroup";
import type { Datasource } from "../api/datasource";
import { computed, reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";

const props = defineProps<{
  visible: boolean;
  mode: "create" | "edit";
  initialValue?: ArchiveGroup | null;
  datasources: Datasource[];
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "submit", payload: ArchiveGroupPayload): void;
}>();

const form = reactive<ArchiveGroupPayload>({
  parentId: undefined,
  groupCode: "",
  groupName: "",
  sourceDatasourceId: 0,
  targetDatasourceId: 0,
  enableStatus: 0,
  triggerMode: "MANUAL",
  remark: ""
});
const errorMessage = ref("");
const groupCodePattern = /^[A-Za-z][A-Za-z0-9_-]{1,63}$/;
const { t } = useI18n();

const title = computed(() =>
  props.mode === "create" ? t("archiveGroup.form.createTitle") : t("archiveGroup.form.editTitle")
);

watch(
  () => [props.visible, props.initialValue, props.mode],
  () => {
    errorMessage.value = "";
    if (props.mode === "edit" && props.initialValue) {
      form.parentId = props.initialValue.parentId;
      form.groupCode = props.initialValue.groupCode || "";
      form.groupName = props.initialValue.groupName || "";
      form.groupPath = props.initialValue.groupPath;
      form.groupLevel = props.initialValue.groupLevel;
      form.sourceDatasourceId = props.initialValue.sourceDatasourceId;
      form.targetDatasourceId = props.initialValue.targetDatasourceId;
      form.ownerUserId = props.initialValue.ownerUserId;
      form.enableStatus = props.initialValue.enableStatus ?? 0;
      form.triggerMode = props.initialValue.triggerMode || "MANUAL";
      form.remark = props.initialValue.remark || "";
      return;
    }
    form.parentId = undefined;
    form.groupCode = "";
    form.groupName = "";
    form.groupPath = undefined;
    form.groupLevel = undefined;
    form.sourceDatasourceId = 0;
    form.targetDatasourceId = 0;
    form.ownerUserId = undefined;
    form.enableStatus = 0;
    form.triggerMode = "MANUAL";
    form.remark = "";
  },
  { immediate: true }
);

function validate(): boolean {
  if (!form.groupCode.trim()) {
    errorMessage.value = t("archiveGroup.form.validation.codeRequired");
    return false;
  }
  if (!groupCodePattern.test(form.groupCode.trim())) {
    errorMessage.value = t("archiveGroup.form.validation.codeInvalid");
    return false;
  }
  if (!form.groupName.trim()) {
    errorMessage.value = t("archiveGroup.form.validation.nameRequired");
    return false;
  }
  if (!form.sourceDatasourceId) {
    errorMessage.value = t("archiveGroup.form.validation.sourceRequired");
    return false;
  }
  if (!form.targetDatasourceId) {
    errorMessage.value = t("archiveGroup.form.validation.targetRequired");
    return false;
  }
  return true;
}

function handleSubmit(): void {
  if (props.submitting) {
    return;
  }
  errorMessage.value = "";
  if (!validate()) {
    return;
  }
  emit("submit", {
    ...form,
    groupCode: form.groupCode.trim(),
    groupName: form.groupName.trim(),
    triggerMode: form.triggerMode?.trim() || "MANUAL",
    remark: form.remark?.trim()
  });
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card">
      <header class="modal-card__header">
        <h3>{{ title }}</h3>
      </header>
      <form class="form-grid" @submit.prevent="handleSubmit">
        <label>{{ t("archiveGroup.form.code") }}<input v-model="form.groupCode" :disabled="submitting || mode === 'edit'" /></label>
        <label>{{ t("archiveGroup.form.name") }}<input v-model="form.groupName" :disabled="submitting" /></label>
        <label>
          {{ t("archiveGroup.form.sourceDatasource") }}
          <select v-model.number="form.sourceDatasourceId" :disabled="submitting">
            <option :value="0">{{ t("archiveGroup.form.selectDatasource") }}</option>
            <option v-for="item in datasources" :key="item.id" :value="item.id">{{ item.datasourceName }}</option>
          </select>
        </label>
        <label>
          {{ t("archiveGroup.form.targetDatasource") }}
          <select v-model.number="form.targetDatasourceId" :disabled="submitting">
            <option :value="0">{{ t("archiveGroup.form.selectDatasource") }}</option>
            <option v-for="item in datasources" :key="item.id" :value="item.id">{{ item.datasourceName }}</option>
          </select>
        </label>
        <label>
          {{ t("archiveGroup.form.status") }}
          <select v-model.number="form.enableStatus" :disabled="submitting">
            <option :value="0">{{ t("status.enabled") }}</option>
            <option :value="1">{{ t("status.disabled") }}</option>
          </select>
        </label>
        <label>{{ t("archiveGroup.form.triggerMode") }}<input v-model="form.triggerMode" :disabled="submitting" /></label>
        <label class="full-width">{{ t("archiveGroup.form.remark") }}<textarea v-model="form.remark" :disabled="submitting" /></label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <footer class="modal-card__footer">
          <button type="button" class="btn btn--subtle" :disabled="submitting" @click="emit('close')">{{ t("common.cancel") }}</button>
          <button type="submit" class="btn btn--primary" :disabled="submitting">
            {{ submitting ? t("common.saving") : t("common.save") }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
