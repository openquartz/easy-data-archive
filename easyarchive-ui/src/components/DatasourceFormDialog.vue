<script setup lang="ts">
import type { Datasource, DatasourcePayload } from "../api/datasource";
import { computed, reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";

const props = defineProps<{
  visible: boolean;
  mode: "create" | "edit";
  initialValue?: Datasource | null;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "submit", payload: DatasourcePayload): void;
}>();

const form = reactive<DatasourcePayload>({
  datasourceCode: "",
  datasourceName: "",
  datasourceType: "MYSQL",
  jdbcUrl: "",
  username: "",
  passwordCipher: "",
  schemaName: "",
  status: 1,
  remark: ""
});
const errorMessage = ref("");
const datasourceCodePattern = /^[A-Za-z][A-Za-z0-9_-]{1,63}$/;
const { t } = useI18n();

const title = computed(() => (props.mode === "create" ? t("datasource.form.createTitle") : t("datasource.form.editTitle")));

watch(
  () => [props.visible, props.initialValue, props.mode],
  () => {
    errorMessage.value = "";
    if (props.mode === "edit" && props.initialValue) {
      form.datasourceCode = props.initialValue.datasourceCode || "";
      form.datasourceName = props.initialValue.datasourceName || "";
      form.datasourceType = props.initialValue.datasourceType || "MYSQL";
      form.jdbcUrl = props.initialValue.jdbcUrl || "";
      form.username = props.initialValue.username || "";
      form.passwordCipher = "";
      form.schemaName = props.initialValue.schemaName || "";
      form.status = props.initialValue.status ?? 1;
      form.remark = props.initialValue.remark || "";
      return;
    }
    form.datasourceCode = "";
    form.datasourceName = "";
    form.datasourceType = "MYSQL";
    form.jdbcUrl = "";
    form.username = "";
    form.passwordCipher = "";
    form.schemaName = "";
    form.status = 1;
    form.remark = "";
  },
  { immediate: true }
);

function validate(): boolean {
  const datasourceCode = form.datasourceCode?.trim() || "";
  const jdbcUrl = form.jdbcUrl?.trim() || "";

  if (!datasourceCode) {
    errorMessage.value = t("datasource.form.validation.codeRequired");
    return false;
  }
  if (!datasourceCodePattern.test(datasourceCode)) {
    errorMessage.value = t("datasource.form.validation.codeInvalid");
    return false;
  }
  if (!form.datasourceName?.trim()) {
    errorMessage.value = t("datasource.form.validation.nameRequired");
    return false;
  }
  if (!form.datasourceType?.trim()) {
    errorMessage.value = t("datasource.form.validation.typeRequired");
    return false;
  }
  if (!jdbcUrl) {
    errorMessage.value = t("datasource.form.validation.jdbcRequired");
    return false;
  }
  if (!jdbcUrl.toLowerCase().startsWith("jdbc:")) {
    errorMessage.value = t("datasource.form.validation.jdbcInvalid");
    return false;
  }
  if (!form.username?.trim()) {
    errorMessage.value = t("datasource.form.validation.usernameRequired");
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
    datasourceCode: form.datasourceCode.trim(),
    datasourceName: form.datasourceName.trim(),
    datasourceType: form.datasourceType.trim(),
    jdbcUrl: form.jdbcUrl.trim(),
    username: form.username.trim(),
    schemaName: form.schemaName?.trim(),
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
        <label>{{ t("datasource.form.code") }}<input v-model="form.datasourceCode" :disabled="submitting || mode === 'edit'" /></label>
        <label>{{ t("datasource.form.name") }}<input v-model="form.datasourceName" :disabled="submitting" /></label>
        <label>{{ t("datasource.form.type") }}<input v-model="form.datasourceType" :disabled="submitting" /></label>
        <label>{{ t("datasource.form.jdbcUrl") }}<input v-model="form.jdbcUrl" :disabled="submitting" /></label>
        <label>{{ t("datasource.form.username") }}<input v-model="form.username" :disabled="submitting" /></label>
        <label>
          {{ t("datasource.form.password") }}
          <input
            v-model="form.passwordCipher"
            :placeholder="mode === 'edit' ? t('datasource.form.keepPassword') : ''"
            :disabled="submitting"
          />
        </label>
        <label>{{ t("datasource.form.schema") }}<input v-model="form.schemaName" :disabled="submitting" /></label>
        <label>
          {{ t("datasource.form.status") }}
          <select v-model.number="form.status" :disabled="submitting">
            <option :value="1">{{ t("status.enabled") }}</option>
            <option :value="0">{{ t("status.disabled") }}</option>
          </select>
        </label>
        <label class="full-width">{{ t("datasource.form.remark") }}<textarea v-model="form.remark" :disabled="submitting" /></label>
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
