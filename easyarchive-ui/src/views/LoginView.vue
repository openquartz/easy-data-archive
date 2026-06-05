<script setup lang="ts">
import AppBrand from "../components/AppBrand.vue";
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import LanguageSwitcher from "../components/LanguageSwitcher.vue";
import { useI18n } from "../i18n";
import { useAuthStore } from "../stores/auth";

interface LoginForm {
  username: string;
  password: string;
}

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const { t } = useI18n();
const form = reactive<LoginForm>({
  username: "",
  password: ""
});
const errorMessage = ref("");
const submitting = ref(false);

function validate(): boolean {
  if (!form.username.trim()) {
    errorMessage.value = t("login.validation.usernameRequired");
    return false;
  }
  if (!form.password.trim()) {
    errorMessage.value = t("login.validation.passwordRequired");
    return false;
  }
  return true;
}

async function handleSubmit(): Promise<void> {
  if (submitting.value) {
    return;
  }
  errorMessage.value = "";
  if (!validate()) {
    return;
  }
  submitting.value = true;
  try {
    await authStore.login({
      username: form.username,
      password: form.password
    });
    const redirect = typeof route.query.redirect === "string" ? route.query.redirect : "/";
    await router.replace(redirect);
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : t("login.failed");
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <div class="login-card__header">
        <AppBrand :title="t('login.title')" :subtitle="t('layout.topbar')" light compact />
        <LanguageSwitcher />
      </div>
      <form class="login-form" @submit.prevent="handleSubmit">
        <label>
          {{ t("login.username") }}
          <input v-model="form.username" type="text" autocomplete="username" :disabled="submitting" />
        </label>
        <label>
          {{ t("login.password") }}
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            :disabled="submitting"
          />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">
          {{ submitting ? t("login.submitting") : t("login.submit") }}
        </button>
      </form>
    </section>
  </main>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
}

.login-card {
  width: min(100%, 420px);
  border: 1px solid #d9dce3;
  border-radius: 18px;
  padding: 28px;
  background: #fff;
  box-shadow: 0 18px 40px rgba(20, 35, 90, 0.1);
}

.login-card__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.login-form {
  display: grid;
  gap: 12px;
}

label {
  display: grid;
  gap: 6px;
  font-size: 14px;
}

input {
  width: 100%;
  height: 36px;
  border: 1px solid #c7ccd6;
  border-radius: 6px;
  padding: 0 10px;
}

button {
  height: 40px;
  border: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #1f7a8c 0%, #165a73 100%);
  color: #fff;
  cursor: pointer;
  font-weight: 600;
}

button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.error {
  margin: 0;
  color: #c62828;
  font-size: 13px;
}
</style>
