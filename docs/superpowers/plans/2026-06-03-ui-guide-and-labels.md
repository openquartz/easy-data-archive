# EasyArchive UI Guide And Labels Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a dedicated operation guide page and update key UI labels and language controls in the Vue frontend.

**Architecture:** Extend the existing Vue Router layout with one new authenticated route, update i18n labels in place, and keep long-form guide content in a focused view component. Preserve existing API contracts by changing only visible labels and locale bootstrapping behavior.

**Tech Stack:** Vue 3, Vue Router 4, TypeScript, Vite

---

### Task 1: Documented prep

**Files:**
- Create: `docs/superpowers/specs/2026-06-03-ui-guide-and-labels-design.md`
- Create: `docs/superpowers/plans/2026-06-03-ui-guide-and-labels.md`

- [ ] **Step 1: Record the approved UI scope**

Write the design scope covering default Chinese locale, dropdown language selector, archive task renaming, datasource connection-address labels, and the new operation guide page.

- [ ] **Step 2: Save the execution plan**

Write this implementation plan so the UI work can be executed in one focused pass.

### Task 2: Update shared app shell behavior

**Files:**
- Modify: `easyarchive-ui/src/i18n/index.ts`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Modify: `easyarchive-ui/src/components/LanguageSwitcher.vue`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`

- [ ] **Step 1: Default locale to Chinese while preserving stored choice**

Change initial locale resolution so first-time visits use `zh-CN` unless a stored locale already exists.

- [ ] **Step 2: Update shared labels**

Rename visible task navigation labels to archive-task wording and change datasource JDBC wording to connection-address wording in both locales.

- [ ] **Step 3: Replace button-based language switching with a select**

Use a single dropdown bound to locale state and keep the existing `setLocale` integration.

### Task 3: Add the operation guide page

**Files:**
- Modify: `easyarchive-ui/src/router/index.ts`
- Create: `easyarchive-ui/src/views/GuideView.vue`

- [ ] **Step 1: Register the guide route**

Add a new authenticated child route under the main app layout.

- [ ] **Step 2: Implement the guide view**

Create a sectioned guide page that covers quick start, datasource setup, archive task setup, rule scenarios, operational advice, and common issues in Chinese and English.

### Task 4: Verify the frontend

**Files:**
- Test: `easyarchive-ui/package.json`

- [ ] **Step 1: Run the build**

Run: `npm run build`
Expected: Vite production build completes successfully without TypeScript errors.

- [ ] **Step 2: Review the changed files**

Confirm only the intended frontend UI files and the new docs were changed for this task.
