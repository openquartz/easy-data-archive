<script setup lang="ts">
import type { User, UserPayload } from "../api/user";
import { computed, reactive, ref, watch } from "vue";

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
  status: 1,
  remark: ""
});
const errorMessage = ref("");
const title = computed(() => (props.mode === "create" ? "Create User" : "Edit User"));

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
      form.status = props.initialValue.status ?? 1;
      form.remark = props.initialValue.remark || "";
      return;
    }
    form.username = "";
    form.password = "";
    form.realName = "";
    form.mobile = "";
    form.email = "";
    form.status = 1;
    form.remark = "";
  },
  { immediate: true }
);

function validate(): boolean {
  if (!form.username.trim()) {
    errorMessage.value = "Username is required";
    return false;
  }
  if (props.mode === "create" && !form.password?.trim()) {
    errorMessage.value = "Password is required";
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
        <label>Username<input v-model="form.username" :disabled="submitting || mode === 'edit'" /></label>
        <label>
          Password
          <input
            v-model="form.password"
            type="password"
            :placeholder="mode === 'edit' ? 'Leave blank to keep unchanged' : ''"
            :disabled="submitting"
          />
        </label>
        <label>Real Name<input v-model="form.realName" :disabled="submitting" /></label>
        <label>Mobile<input v-model="form.mobile" :disabled="submitting" /></label>
        <label>Email<input v-model="form.email" type="email" :disabled="submitting" /></label>
        <label>
          Status
          <select v-model.number="form.status" :disabled="submitting">
            <option :value="1">Enabled</option>
            <option :value="0">Disabled</option>
          </select>
        </label>
        <label class="full-width">Remark<textarea v-model="form.remark" :disabled="submitting" /></label>
        <p v-if="errorMessage" class="error">{{ errorMessage }}</p>
        <footer class="modal-card__footer">
          <button type="button" class="btn btn--subtle" :disabled="submitting" @click="emit('close')">Cancel</button>
          <button type="submit" class="btn btn--primary" :disabled="submitting">
            {{ submitting ? "Saving..." : "Save" }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
