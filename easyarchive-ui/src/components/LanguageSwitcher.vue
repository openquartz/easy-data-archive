<script setup lang="ts">
import { useI18n, type Locale } from "../i18n";

const { locale, locales, setLocale, t } = useI18n();

function switchLocale(nextLocale: Locale): void {
  if (locale.value === nextLocale) {
    return;
  }
  setLocale(nextLocale);
}
</script>

<template>
  <label class="language-switcher">
    <span class="language-switcher__label">{{ t("language.switch") }}</span>
    <select
      :value="locale"
      class="language-switcher__select"
      @change="switchLocale(($event.target as HTMLSelectElement).value as Locale)"
    >
      <option v-for="item in locales" :key="item" :value="item">
        {{ item === "zh-CN" ? t("language.zhCN") : t("language.enUS") }}
      </option>
    </select>
  </label>
</template>

<style scoped>
.language-switcher {
  display: inline-grid;
  gap: 8px;
}

.language-switcher__label {
  font-size: 12px;
  color: #5f6b7a;
}

.language-switcher__select {
  min-width: 96px;
  height: 34px;
  border: 1px solid rgba(16, 57, 71, 0.16);
  border-radius: 10px;
  padding: 0 10px;
  background: #fff;
  color: #164b5a;
  cursor: pointer;
  transition: all 160ms ease;
}
</style>
