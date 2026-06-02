<script setup lang="ts">
import { useI18n, type Locale } from "../i18n";

const { locale, setLocale, t } = useI18n();

function switchLocale(nextLocale: Locale): void {
  if (locale.value === nextLocale) {
    return;
  }
  setLocale(nextLocale);
}
</script>

<template>
  <div class="language-switcher" :aria-label="t('language.switch')">
    <button
      type="button"
      class="language-switcher__button"
      :class="{ 'language-switcher__button--active': locale === 'zh-CN' }"
      @click="switchLocale('zh-CN')"
    >
      {{ t("language.zhCN") }}
    </button>
    <button
      type="button"
      class="language-switcher__button"
      :class="{ 'language-switcher__button--active': locale === 'en-US' }"
      @click="switchLocale('en-US')"
    >
      {{ t("language.enUS") }}
    </button>
  </div>
</template>

<style scoped>
.language-switcher {
  display: inline-flex;
  gap: 8px;
}

.language-switcher__button {
  min-width: 64px;
  height: 32px;
  border: 1px solid rgba(16, 57, 71, 0.16);
  border-radius: 999px;
  background: #fff;
  color: #164b5a;
  cursor: pointer;
  transition: all 160ms ease;
}

.language-switcher__button--active {
  border-color: #155f72;
  background: #155f72;
  color: #fff;
}
</style>
