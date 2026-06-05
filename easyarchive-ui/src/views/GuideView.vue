<script setup lang="ts">
import { computed } from "vue";
import { buildGuideSections } from "../content/guideContent";
import { useI18n } from "../i18n";
const { locale, isZhCN } = useI18n();
const sections = computed(() => buildGuideSections(locale.value));
</script>

<template>
  <section class="guide-page">
    <aside class="guide-page__nav page-card">
      <h1 class="guide-page__title">{{ isZhCN ? "操作指南" : "Operation Guide" }}</h1>
      <p class="guide-page__intro">
        {{
          isZhCN
            ? "按章节了解系统定位、快速上手步骤、规则配置方式和常见风险点。"
            : "Use the sections below to understand setup flow, rule configuration, and common operational risks."
        }}
      </p>
      <nav class="guide-page__anchors">
        <a v-for="section in sections" :key="section.id" class="guide-page__anchor" :href="`#${section.id}`">
          {{ section.title }}
        </a>
      </nav>
    </aside>

    <div class="guide-page__content">
      <article v-for="section in sections" :id="section.id" :key="section.id" class="page-card guide-section">
        <h2 class="guide-section__title">{{ section.title }}</h2>
        <p v-if="section.intro" class="guide-section__intro">{{ section.intro }}</p>
        <ul v-if="section.items?.length" class="guide-section__list">
          <li v-for="item in section.items" :key="item">{{ item }}</li>
        </ul>
        <div v-if="section.notes?.length" class="guide-section__notes">
          <strong>{{ isZhCN ? "补充建议" : "Notes" }}</strong>
          <ul class="guide-section__list">
            <li v-for="note in section.notes" :key="note">{{ note }}</li>
          </ul>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.guide-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
}

.guide-page__nav {
  position: sticky;
  top: 24px;
  align-self: start;
}

.guide-page__title {
  margin: 0 0 10px;
  font-size: 1.25rem;
}

.guide-page__intro {
  margin: 0 0 16px;
  color: var(--ea-text-muted);
  line-height: 1.6;
}

.guide-page__anchors {
  display: grid;
  gap: 8px;
}

.guide-page__anchor {
  color: var(--ea-primary);
  text-decoration: none;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f5fafb;
  border: 1px solid rgba(31, 122, 140, 0.08);
}

.guide-page__content {
  display: grid;
  gap: 16px;
}

.guide-section__title {
  margin: 0 0 10px;
  font-size: 1.1rem;
}

.guide-section__intro {
  margin: 0 0 12px;
  color: var(--ea-text-muted);
  line-height: 1.7;
}

.guide-section__list {
  margin: 0;
  padding-left: 20px;
  display: grid;
  gap: 10px;
  line-height: 1.8;
}

.guide-section__notes {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, rgba(31, 122, 140, 0.06), rgba(31, 122, 140, 0.02));
  border: 1px solid rgba(31, 122, 140, 0.12);
}

@media (max-width: 960px) {
  .guide-page {
    grid-template-columns: 1fr;
  }

  .guide-page__nav {
    position: static;
  }
}
</style>
