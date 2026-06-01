<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";

interface LoginForm {
  username: string;
  password: string;
}

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();
const form = reactive<LoginForm>({
  username: "",
  password: ""
});
const errorMessage = ref("");
const submitting = ref(false);

function validate(): boolean {
  if (!form.username.trim()) {
    errorMessage.value = "Username is required";
    return false;
  }
  if (!form.password.trim()) {
    errorMessage.value = "Password is required";
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
    errorMessage.value = error instanceof Error ? error.message : "Login failed";
  } finally {
    submitting.value = false;
  }
}
</script>

<template>
  <main class="login-page">
    <section class="login-card">
      <h1>EasyArchive Login</h1>
      <form class="login-form" @submit.prevent="handleSubmit">
        <label>
          Username
          <input v-model="form.username" type="text" autocomplete="username" :disabled="submitting" />
        </label>
        <label>
          Password
          <input
            v-model="form.password"
            type="password"
            autocomplete="current-password"
            :disabled="submitting"
          />
        </label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? "Signing in..." : "Sign in" }}</button>
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
  width: min(100%, 360px);
  border: 1px solid #d9dce3;
  border-radius: 12px;
  padding: 24px;
  background: #fff;
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
  height: 36px;
  border: 0;
  border-radius: 6px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
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

