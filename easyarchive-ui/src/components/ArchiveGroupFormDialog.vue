<script setup lang="ts">
import type { ArchiveGroup, ArchiveGroupPayload } from "../api/archiveGroup";
import type { Datasource } from "../api/datasource";
import type { User } from "../api/user";
import { computed, reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";
import {
  createArchiveGroupFormValue,
  createOwnerOptions,
  isNotificationConfigEditable,
  requiresWebhook
} from "../utils/archiveGroupForm";
import { useAuthStore } from "../stores/auth";
import { normalizeRoleCode } from "../constants/roles";

const props = defineProps<{
  visible: boolean;
  mode: "create" | "edit";
  initialValue?: ArchiveGroup | null;
  datasources: Datasource[];
  users: User[];
  submitting?: boolean;
  ownerReadonly?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "submit", payload: ArchiveGroupPayload): void;
}>();

const form = reactive<ArchiveGroupPayload>(createArchiveGroupFormValue());
const errorMessage = ref("");
const groupCodePattern = /^[A-Za-z][A-Za-z0-9_-]{1,63}$/;
const { t } = useI18n();
const authStore = useAuthStore();
const ownerOptions = computed(() => createOwnerOptions(props.users, props.initialValue));
const ownerReadonly = computed(() =>
  props.ownerReadonly ?? normalizeRoleCode(authStore.profile?.roleCode) === "normal_user"
);

const title = computed(() =>
  props.mode === "create" ? t("archiveGroup.form.createTitle") : t("archiveGroup.form.editTitle")
);

watch(
  () => [props.visible, props.initialValue, props.mode],
  () => {
    errorMessage.value = "";
    const nextValue = props.mode === "edit" && props.initialValue
      ? createArchiveGroupFormValue(props.initialValue)
      : createArchiveGroupFormValue();
    Object.assign(form, nextValue);
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
  if (!form.ownerUserId) {
    errorMessage.value = t("archiveGroup.form.validation.ownerRequired");
    return false;
  }
  if (isNotificationConfigEditable(form) && !form.notifyChannel) {
    errorMessage.value = t("archiveGroup.form.validation.notifyChannelRequired");
    return false;
  }
  if (requiresWebhook(form) && !form.notifyWebhookUrl?.trim()) {
    errorMessage.value = t("archiveGroup.form.validation.notifyWebhookRequired");
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
    notifyWebhookUrl: form.notifyWebhookUrl?.trim(),
    remark: form.remark?.trim()
  });
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card modal-card--resizable">
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
        <label>
          {{ t("archiveGroup.form.owner") }}
          <select v-model.number="form.ownerUserId" :disabled="submitting || ownerReadonly">
            <option :value="undefined">{{ t("archiveGroup.form.selectOwner") }}</option>
            <option v-for="user in ownerOptions" :key="user.id" :value="user.id">
              {{ user.realName || user.username }} ({{ user.username }})
            </option>
          </select>
        </label>
        <label>
          {{ t("archiveGroup.form.notifyEnabled") }}
          <select v-model.number="form.notifyEnabled" :disabled="submitting">
            <option :value="0">{{ t("common.no") }}</option>
            <option :value="1">{{ t("common.yes") }}</option>
          </select>
        </label>
        <label>
          {{ t("archiveGroup.form.notifyChannel") }}
          <select v-model="form.notifyChannel" :disabled="submitting || !isNotificationConfigEditable(form)">
            <option :value="undefined">{{ t("archiveGroup.form.selectNotifyChannel") }}</option>
            <option value="IN_APP">{{ t("archiveGroup.form.notifyChannels.inApp") }}</option>
            <option value="FEISHU">{{ t("archiveGroup.form.notifyChannels.feishu") }}</option>
            <option value="WECOM">{{ t("archiveGroup.form.notifyChannels.wecom") }}</option>
          </select>
        </label>
        <label v-if="requiresWebhook(form)" class="full-width">
          {{ t("archiveGroup.form.notifyWebhookUrl") }}
          <input v-model="form.notifyWebhookUrl" :disabled="submitting || !requiresWebhook(form)" />
        </label>
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

<style scoped>
.modal-card--resizable {
  width: min(880px, calc(100vw - 3rem));
  min-width: 560px;
  min-height: 360px;
  max-width: 96vw;
  max-height: 90vh;
  overflow: auto;
  resize: both;
}

.modal-card--resizable .form-grid {
  align-content: start;
}

.modal-card--resizable textarea {
  min-height: 8rem;
  resize: vertical;
}

@media (max-width: 640px) {
  .modal-card--resizable {
    min-width: 0;
    width: calc(100vw - 1.5rem);
    max-height: 92vh;
    resize: none;
  }
}
</style>
