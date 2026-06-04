<script setup lang="ts">
import type { User, UserPayload } from "../api/user";
import { computed, reactive, ref, watch } from "vue";
import { useI18n } from "../i18n";

const props = defineProps<{
  visible: boolean;
  mode: "create" | "edit";
  initialValue?: User | null;
  submitting?: boolean;
}>();

const emit = defineEmits<{
  (event: "close"): void;
  (event: "submit", payload: UserPayload): void;
}>();

const form = reactive<UserPayload>({
  username: "",
  password: "",
  realName: "",
  mobile: "",
  email: "",
  roleCode: "USER",
  status: 0,
  remark: ""
});
const errorMessage = ref("");
const { t } = useI18n();
const title = computed(() => (props.mode === "create" ? t("user.form.createTitle") : t("user.form.editTitle")));
const usernamePattern = /^[A-Za-z][A-Za-z0-9_.-]{2,31}$/;
const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const mobilePattern = /^\+?[0-9\- ]{7,20}$/;

watch(
  () => [props.visible, props.mode, props.initialValue],
  () => {
    errorMessage.value = "";
    if (props.mode === "edit" && props.initialValue) {
      form.username = props.initialValue.username || "";
      form.password = "";
      form.realName = props.initialValue.realName || "";
      form.mobile = props.initialValue.mobile || "";
      form.email = props.initialValue.email || "";
      form.roleCode = props.initialValue.roleCode || "USER";
      form.status = props.initialValue.status ?? 0;
      form.remark = props.initialValue.remark || "";
      return;
    }
    form.username = "";
    form.password = "";
    form.realName = "";
    form.mobile = "";
    form.email = "";
    form.roleCode = "USER";
    form.status = 0;
    form.remark = "";
  },
  { immediate: true }
);

function validate(): boolean {
  const username = form.username.trim();
  const email = form.email?.trim() || "";
  const mobile = form.mobile?.trim() || "";

  if (!username) {
    errorMessage.value = t("user.form.validation.usernameRequired");
    return false;
  }
  if (!usernamePattern.test(username)) {
    errorMessage.value = t("user.form.validation.usernameInvalid");
    return false;
  }
  if (props.mode === "create" && !form.password?.trim()) {
    errorMessage.value = t("user.form.validation.passwordRequired");
    return false;
  }
  if (email && !emailPattern.test(email)) {
    errorMessage.value = t("user.form.validation.emailInvalid");
    return false;
  }
  if (mobile && !mobilePattern.test(mobile)) {
    errorMessage.value = t("user.form.validation.mobileInvalid");
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
    username: form.username.trim(),
    password: form.password?.trim() || undefined,
    realName: form.realName?.trim(),
    mobile: form.mobile?.trim(),
    email: form.email?.trim(),
    roleCode: form.roleCode?.trim() || "USER",
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
        <label>{{ t("user.form.username") }}<input v-model="form.username" :disabled="submitting || mode === 'edit'" /></label>
        <label>
          {{ t("user.form.password") }}
          <input
            v-model="form.password"
            type="password"
            :placeholder="mode === 'edit' ? t('user.form.keepPassword') : ''"
            :disabled="submitting"
          />
        </label>
        <label>{{ t("user.form.realName") }}<input v-model="form.realName" :disabled="submitting" /></label>
        <label>{{ t("user.form.mobile") }}<input v-model="form.mobile" :disabled="submitting" /></label>
        <label>{{ t("user.form.email") }}<input v-model="form.email" type="email" :disabled="submitting" /></label>
        <label>
          {{ t("user.form.roleCode") }}
          <select v-model="form.roleCode" :disabled="submitting">
            <option value="USER">{{ t("user.roles.USER") }}</option>
            <option value="ADMIN">{{ t("user.roles.ADMIN") }}</option>
          </select>
        </label>
        <label>
          {{ t("user.form.status") }}
          <select v-model.number="form.status" :disabled="submitting">
            <option :value="0">{{ t("status.enabled") }}</option>
            <option :value="1">{{ t("status.disabled") }}</option>
          </select>
        </label>
        <label class="full-width">{{ t("user.form.remark") }}<textarea v-model="form.remark" :disabled="submitting" /></label>
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
