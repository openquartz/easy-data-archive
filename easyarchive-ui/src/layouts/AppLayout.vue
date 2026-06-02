<script setup lang="ts">
import LanguageSwitcher from "../components/LanguageSwitcher.vue";
import { useI18n } from "../i18n";

const { t } = useI18n();
</script>

<template>
  <div class="app-shell">
    <aside class="app-shell__sidebar">
      <div class="brand">{{ t("layout.brand") }}</div>
      <nav class="nav">
        <RouterLink class="nav__item" :to="{ name: 'dashboard' }">{{ t("layout.nav.dashboard") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'datasources' }">{{ t("layout.nav.datasources") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'tasks' }">{{ t("layout.nav.tasks") }}</RouterLink>
        <RouterLink class="nav__item" :to="{ name: 'users' }">{{ t("layout.nav.users") }}</RouterLink>
        <RouterLink class="nav__item nav__item--ghost" :to="{ name: 'login' }">{{ t("layout.nav.login") }}</RouterLink>
      </nav>
    </aside>
    <div class="app-shell__main">
      <header class="app-shell__topbar">
        <strong>{{ t("layout.topbar") }}</strong>
        <LanguageSwitcher />
      </header>
      <main class="app-shell__content">
        <router-view />
      </main>
    </div>
  </div>
</template>
