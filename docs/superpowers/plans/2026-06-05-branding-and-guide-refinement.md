# Branding And Guide Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Update the frontend brand to `数据归档平台`, add a matching logo, reorder the sidebar, and expand the guide page into a technical manual with regression coverage.

**Architecture:** Extract brand, navigation, and guide content into focused frontend modules that can be tested with the existing Node test setup. Update Vue layout and page components to consume those modules, then verify behavior with targeted tests and a production build.

**Tech Stack:** Vue 3, TypeScript, Vite, Node `test`, existing i18n utilities

---

### Task 1: Add failing tests for branding, navigation, and guide structure

**Files:**
- Create: `easyarchive-ui/tests/branding-and-guide.test.ts`
- Modify: `easyarchive-ui/package.json`
- Test: `easyarchive-ui/tests/branding-and-guide.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import test from "node:test";
import assert from "node:assert/strict";

import { messages } from "../src/i18n/messages";
import { buildGuideSections } from "../src/content/guideContent";
import { buildPrimaryNavItems } from "../src/content/navigation";

test("brand copy uses 数据归档平台 across visible entry points", () => {
  assert.equal(messages["zh-CN"].layout.brand, "数据归档平台");
  assert.equal(messages["zh-CN"].login.title, "数据归档平台");
  assert.equal(messages["en-US"].layout.brand, "Data Archive Platform");
});

test("sidebar puts guide immediately after dashboard", () => {
  const items = buildPrimaryNavItems(true);
  assert.deepEqual(
    items.map((item) => item.routeName),
    ["dashboard", "guide", "datasources", "archive-groups", "tasks", "operation-logs", "users"]
  );
});

test("guide content includes technical-manual sections and safety notes", () => {
  const sections = buildGuideSections("zh-CN");
  assert.deepEqual(
    sections.map((section) => section.id),
    [
      "positioning",
      "preparation",
      "datasource-config",
      "group-config",
      "rule-overview",
      "id-rule",
      "time-rule",
      "query-cleanup",
      "examples-validation",
      "troubleshooting"
    ]
  );
  assert.ok(
    sections.some((section) =>
      section.notes?.some((note) => note.includes("先开启写入，关闭清理"))
    )
  );
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test easyarchive-ui/tests/branding-and-guide.test.ts`
Expected: FAIL because `guideContent` and `navigation` modules do not exist yet and current brand strings still use `EasyArchive`.

- [ ] **Step 3: Add a reusable test script**

```json
"scripts": {
  "dev": "vite",
  "build": "vue-tsc -b && vite build",
  "test": "node --test tests/*.test.ts",
  "test:auth-contract": "node ./scripts/auth-contract-check.mjs",
  "test:user-status-contract": "node ./scripts/user-status-contract-check.mjs",
  "preview": "vite preview",
  "smoke": "npm run build && node ./scripts/smoke-check.mjs"
}
```

- [ ] **Step 4: Re-run the failing test**

Run: `npm test -- --test-name-pattern=branding`
Expected: FAIL with missing module or assertion failures, proving the tests are exercising the new behavior.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-ui/package.json easyarchive-ui/tests/branding-and-guide.test.ts
git commit -m "test: cover branding and guide structure"
```

### Task 2: Implement data modules and brand copy updates

**Files:**
- Create: `easyarchive-ui/src/content/navigation.ts`
- Create: `easyarchive-ui/src/content/guideContent.ts`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Modify: `easyarchive-ui/index.html`
- Test: `easyarchive-ui/tests/branding-and-guide.test.ts`

- [ ] **Step 1: Create navigation data module**

```ts
export type PrimaryNavItem = {
  routeName:
    | "dashboard"
    | "guide"
    | "datasources"
    | "archive-groups"
    | "tasks"
    | "operation-logs"
    | "users";
  labelKey: string;
  adminOnly?: boolean;
};

const PRIMARY_NAV_ITEMS: PrimaryNavItem[] = [
  { routeName: "dashboard", labelKey: "layout.nav.dashboard" },
  { routeName: "guide", labelKey: "layout.nav.guide" },
  { routeName: "datasources", labelKey: "layout.nav.datasources" },
  { routeName: "archive-groups", labelKey: "layout.nav.archiveGroups" },
  { routeName: "tasks", labelKey: "layout.nav.tasks" },
  { routeName: "operation-logs", labelKey: "layout.nav.operationLogs", adminOnly: true },
  { routeName: "users", labelKey: "layout.nav.users", adminOnly: true }
];

export function buildPrimaryNavItems(isAdmin: boolean): PrimaryNavItem[] {
  return PRIMARY_NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin);
}
```

- [ ] **Step 2: Create guide content data module**

```ts
import type { Locale } from "../i18n";

export type GuideSection = {
  id: string;
  title: string;
  intro?: string;
  items?: string[];
  notes?: string[];
};

const zhCNSections: GuideSection[] = [/* finalized technical-manual sections */];
const enUSSections: GuideSection[] = [/* aligned English summary sections */];

export function buildGuideSections(locale: Locale): GuideSection[] {
  return locale === "zh-CN" ? zhCNSections : enUSSections;
}
```

- [ ] **Step 3: Update visible brand copy**

```ts
layout: {
  brand: "Data Archive Platform",
  // ...
},
login: {
  title: "Data Archive Platform",
  // ...
}
```

```ts
layout: {
  brand: "数据归档平台",
  // ...
},
login: {
  title: "数据归档平台",
  // ...
}
```

```html
<title>数据归档平台</title>
```

- [ ] **Step 4: Run tests to verify the new modules satisfy the behavior**

Run: `node --test easyarchive-ui/tests/branding-and-guide.test.ts`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add easyarchive-ui/src/content/navigation.ts easyarchive-ui/src/content/guideContent.ts easyarchive-ui/src/i18n/messages.ts easyarchive-ui/index.html
git commit -m "feat: add brand and guide content models"
```

### Task 3: Apply logo, sidebar, and guide UI updates

**Files:**
- Create: `easyarchive-ui/src/components/AppBrand.vue`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/views/LoginView.vue`
- Modify: `easyarchive-ui/src/views/GuideView.vue`
- Modify: `easyarchive-ui/src/styles/theme.css`
- Test: `easyarchive-ui/tests/branding-and-guide.test.ts`

- [ ] **Step 1: Add the brand component with inline SVG**

```vue
<template>
  <div class="app-brand">
    <svg viewBox="0 0 64 64" aria-hidden="true" class="app-brand__mark">
      <!-- rounded square, data streams, layered archive base -->
    </svg>
    <div class="app-brand__text">
      <strong>{{ title }}</strong>
      <span>{{ subtitle }}</span>
    </div>
  </div>
</template>
```

- [ ] **Step 2: Update the app layout to consume the nav data**

```ts
import AppBrand from "../components/AppBrand.vue";
import { buildPrimaryNavItems } from "../content/navigation";

const primaryNavItems = computed(() =>
  buildPrimaryNavItems(Boolean(authStore.profile?.isAdmin))
);
```

```vue
<AppBrand :title="t('layout.brand')" :subtitle="t('layout.topbar')" />
<RouterLink
  v-for="item in primaryNavItems"
  :key="item.routeName"
  class="nav__item"
  :to="{ name: item.routeName }"
>
  {{ t(item.labelKey) }}
</RouterLink>
```

- [ ] **Step 3: Update login and guide page presentation**

```vue
<AppBrand :title="t('layout.brand')" :subtitle="t('login.subtitle')" compact />
```

```ts
import { buildGuideSections } from "../content/guideContent";

const sections = computed(() => buildGuideSections(isZhCN.value ? "zh-CN" : "en-US"));
```

```vue
<div v-if="section.notes?.length" class="guide-section__notes guide-section__notes--highlight">
```

- [ ] **Step 4: Adjust shared styling**

```css
.app-brand { /* logo-text layout */ }
.app-brand__mark { /* fixed size and contrast */ }
.guide-section__notes--highlight { /* subtle accent background */ }
@media (max-width: 960px) {
  .guide-page {
    grid-template-columns: 1fr;
  }
}
```

- [ ] **Step 5: Run tests and build**

Run: `node --test easyarchive-ui/tests/branding-and-guide.test.ts && npm --prefix easyarchive-ui run build`
Expected: PASS for tests and a successful Vite production build.

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui/src/components/AppBrand.vue easyarchive-ui/src/layouts/AppLayout.vue easyarchive-ui/src/views/LoginView.vue easyarchive-ui/src/views/GuideView.vue easyarchive-ui/src/styles/theme.css
git commit -m "feat: refresh branding and guide ui"
```
