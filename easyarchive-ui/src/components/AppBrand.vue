<script setup lang="ts">
defineProps<{
  title: string;
  subtitle?: string;
  compact?: boolean;
  light?: boolean;
}>();
</script>

<template>
  <div class="app-brand" :class="{ 'app-brand--compact': compact, 'app-brand--light': light }">
    <svg viewBox="0 0 64 64" class="app-brand__mark" aria-hidden="true">
      <rect x="6" y="6" width="52" height="52" rx="16" fill="rgba(255, 255, 255, 0.14)" />
      <path
        d="M20 20C20 17.8 21.8 16 24 16H40C42.2 16 44 17.8 44 20V21C44 23.2 42.2 25 40 25H24C21.8 25 20 23.2 20 21V20Z"
        fill="#49bfd6"
      />
      <path d="M26 25V33" stroke="#d8fbff" stroke-width="3.5" stroke-linecap="round" />
      <path d="M32 25V37" stroke="#d8fbff" stroke-width="3.5" stroke-linecap="round" />
      <path d="M38 25V33" stroke="#d8fbff" stroke-width="3.5" stroke-linecap="round" />
      <path
        d="M18 36C18 34.9 18.9 34 20 34H44C45.1 34 46 34.9 46 36C46 37.1 45.1 38 44 38H20C18.9 38 18 37.1 18 36Z"
        fill="#d7f5fb"
      />
      <path
        d="M16 42C16 40.9 16.9 40 18 40H46C47.1 40 48 40.9 48 42C48 43.1 47.1 44 46 44H18C16.9 44 16 43.1 16 42Z"
        fill="#9addeb"
      />
      <path
        d="M14 48C14 46.9 14.9 46 16 46H48C49.1 46 50 46.9 50 48C50 49.1 49.1 50 48 50H16C14.9 50 14 49.1 14 48Z"
        fill="#4faec1"
      />
    </svg>
    <div class="app-brand__text">
      <strong>{{ title }}</strong>
      <span v-if="subtitle">{{ subtitle }}</span>
    </div>
  </div>
</template>

<style scoped>
.app-brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-brand--compact {
  gap: 10px;
}

.app-brand__mark {
  width: 48px;
  height: 48px;
  flex: none;
}

.app-brand--compact .app-brand__mark {
  width: 44px;
  height: 44px;
}

.app-brand__text {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.app-brand__text strong,
.app-brand__text span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-brand__text strong {
  font-size: 1rem;
  line-height: 1.2;
}

.app-brand__text span {
  color: rgba(238, 248, 251, 0.72);
  font-size: 12px;
}

.app-brand--light .app-brand__text span {
  color: var(--ea-text-muted);
}

.app-brand--light .app-brand__text strong {
  color: var(--ea-text);
}
</style>
