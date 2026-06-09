<script setup lang="ts">
import { useI18n, type Locale } from "../i18n";

const { locale, setLocale } = useI18n();

const options: { value: Locale; label: string }[] = [
  { value: "zh-CN", label: "中" },
  { value: "en-US", label: "EN" },
];
</script>

<template>
  <div class="language-switcher">
    <button
      v-for="opt in options"
      :key="opt.value"
      class="language-switcher__btn"
      :class="{ 'language-switcher__btn--active': locale === opt.value }"
      @click="setLocale(opt.value)"
    >
      {{ opt.label }}
    </button>
  </div>
</template>

<style scoped>
.language-switcher {
  display: inline-flex;
  border: 1px solid rgba(16, 57, 71, 0.16);
  border-radius: 10px;
  overflow: hidden;
  background: #fff;
}

.language-switcher__btn {
  min-width: 48px;
  height: 34px;
  border: none;
  background: transparent;
  color: #5f6b7a;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 160ms ease;
  padding: 0 12px;
}

.language-switcher__btn--active {
  background: #164b5a;
  color: #fff;
}
</style>
