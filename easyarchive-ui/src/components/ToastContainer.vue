<script setup lang="ts">
import { useI18n } from "../i18n";
import { removeToast, toastStore } from "../stores/toast";

const { t } = useI18n();
</script>

<template>
  <div v-if="toastStore.length" class="toast-stack" aria-live="polite" aria-atomic="true">
    <article
      v-for="item in toastStore"
      :key="item.id"
      class="toast"
      :class="[`toast--${item.type}`]"
      role="status"
    >
      <p>{{ item.message }}</p>
      <button
        type="button"
        class="toast__close"
        :aria-label="t('common.dismiss')"
        @click="removeToast(item.id)"
      >
        ×
      </button>
    </article>
  </div>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  top: 1.25rem;
  right: 1.25rem;
  z-index: 2000;
  display: grid;
  gap: 0.75rem;
  width: min(24rem, calc(100vw - 2rem));
  pointer-events: none;
}

.toast {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 0.75rem;
  align-items: start;
  padding: 0.95rem 1rem;
  border-radius: 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.16);
  backdrop-filter: blur(18px);
  pointer-events: auto;
}

.toast p {
  margin: 0;
  line-height: 1.5;
  font-size: 0.95rem;
}

.toast--success {
  background: rgba(236, 253, 245, 0.94);
  border-color: rgba(16, 185, 129, 0.28);
  color: #065f46;
}

.toast--error {
  background: rgba(254, 242, 242, 0.96);
  border-color: rgba(239, 68, 68, 0.22);
  color: #991b1b;
}

.toast__close {
  border: 0;
  background: transparent;
  color: inherit;
  cursor: pointer;
  font-size: 1.2rem;
  line-height: 1;
  padding: 0.05rem;
}

@media (max-width: 640px) {
  .toast-stack {
    top: 0.75rem;
    right: 0.75rem;
    left: 0.75rem;
    width: auto;
  }
}
</style>
