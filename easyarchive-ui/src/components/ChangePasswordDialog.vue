<script setup lang="ts">
import { reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";
import { changePasswordApi } from "../api/auth";

const props = defineProps<{
  visible: boolean;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "password-changed"): void;
}>();

const { t } = useI18n();

const form = reactive({
  newPassword: "",
  confirmPassword: ""
});

const errorMessage = ref("");

const passwordPattern = /^(?=.*[a-zA-Z])(?=.*\d).{8,}$/;

watch(
  () => props.visible,
  (next) => {
    if (next) {
      form.newPassword = "";
      form.confirmPassword = "";
      errorMessage.value = "";
    }
  }
);

function validate(): boolean {
  if (!form.newPassword) {
    errorMessage.value = t("changePassword.validation.required");
    return false;
  }
  if (form.newPassword.length < 8) {
    errorMessage.value = t("changePassword.validation.minLength");
    return false;
  }
  if (!passwordPattern.test(form.newPassword)) {
    errorMessage.value = t("changePassword.validation.pattern");
    return false;
  }
  if (form.newPassword !== form.confirmPassword) {
    errorMessage.value = t("changePassword.validation.mismatch");
    return false;
  }
  return true;
}

async function handleSubmit(): Promise<void> {
  if (props.submitting) {
    return;
  }
  errorMessage.value = "";
  if (!validate()) {
    return;
  }
  try {
    await changePasswordApi(form.newPassword);
    emit("password-changed");
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : t("changePassword.failed");
    errorMessage.value = message;
  }
}
</script>

<template>
  <div v-if="visible" class="modal-backdrop" @click.self="emit('close')">
    <section class="modal-card">
      <header class="modal-card__header">
        <h3>{{ t("changePassword.title") }}</h3>
      </header>
      <form class="form-grid" @submit.prevent="handleSubmit">
        <label>
          {{ t("changePassword.newPassword") }}
          <input
            v-model="form.newPassword"
            type="password"
            :disabled="submitting"
            autocomplete="new-password"
          />
        </label>
        <label>
          {{ t("changePassword.confirmPassword") }}
          <input
            v-model="form.confirmPassword"
            type="password"
            :disabled="submitting"
            autocomplete="new-password"
          />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <footer class="modal-card__footer">
          <button type="button" class="btn btn--subtle" :disabled="submitting" @click="emit('close')">
            {{ t("common.cancel") }}
          </button>
          <button type="submit" class="btn btn--primary" :disabled="submitting">
            {{ submitting ? t("common.saving") : t("common.save") }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>

<style scoped>
.modal-card {
  width: min(480px, calc(100vw - 3rem));
}

.modal-card .form-grid {
  align-content: start;
}
</style>
