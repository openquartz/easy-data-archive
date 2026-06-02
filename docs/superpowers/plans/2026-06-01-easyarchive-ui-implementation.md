# EasyArchive UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build `easyarchive-ui` as an independent Vue 3 + TypeScript operations console and integrate existing backend APIs with stable request/error/auth handling.

**Architecture:** Add a new frontend module in this monorepo and keep backend in `easyarchive-starter`. Backend gets minimal adapter APIs for dashboard aggregation and response consistency. Frontend uses unified API client, auth store, dictionary mapping, guarded routes, and focused pages.

**Tech Stack:** Vue 3, TypeScript, Vite, Vue Router, Pinia, Axios, Element Plus, Spring Boot 2.7, MyBatis.

---

### Task 1: Scaffold `easyarchive-ui` Module and Base App Shell

**Files:**
- Create: `easyarchive-ui/package.json`
- Create: `easyarchive-ui/package-lock.json`
- Create: `easyarchive-ui/.gitignore`
- Create: `easyarchive-ui/tsconfig.json`
- Create: `easyarchive-ui/vite.config.ts`
- Create: `easyarchive-ui/index.html`
- Create: `easyarchive-ui/src/main.ts`
- Create: `easyarchive-ui/src/App.vue`
- Create: `easyarchive-ui/src/env.d.ts`
- Create: `easyarchive-ui/src/router/index.ts`
- Create: `easyarchive-ui/src/stores/auth.ts`
- Create: `easyarchive-ui/src/layouts/AppLayout.vue`
- Create: `easyarchive-ui/src/styles/theme.css`

- [ ] **Step 1: Create Vite + Vue3 + TS project files**

```bash
mkdir -p easyarchive-ui/src/{api,components,layouts,router,stores,styles,types,utils,views}
```

- [ ] **Step 2: Add dependencies and scripts**

```json
{
  "name": "easyarchive-ui",
  "private": true,
  "version": "0.1.0",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview"
  }
}
```

- [ ] **Step 3: Build base app wiring (router + pinia + theme)**

```ts
const app = createApp(App)
app.use(createPinia())
app.use(router)
app.mount('#app')
```

- [ ] **Step 4: Add base layout with sidebar/topbar/content outlet**

```vue
<router-view />
```

- [ ] **Step 5: Verify dev startup**

Run: `cd easyarchive-ui && npm install && npm run build`  
Expected: build succeeds with no TypeScript errors

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui
git commit -m "feat(ui): scaffold easyarchive-ui module with base app shell"
```

### Task 2: Unified API Client, Auth Flow, and Route Guard

**Files:**
- Create: `easyarchive-ui/src/utils/http.ts`
- Create: `easyarchive-ui/src/api/auth.ts`
- Create: `easyarchive-ui/src/types/api.ts`
- Modify: `easyarchive-ui/src/stores/auth.ts`
- Modify: `easyarchive-ui/src/router/index.ts`
- Create: `easyarchive-ui/src/views/LoginView.vue`

- [ ] **Step 1: Define `ApiResponse<T>` and API error model**

```ts
export interface ApiResponse<T> { code: string; message: string; requestId?: string; data: T }
```

- [ ] **Step 2: Implement axios instance with token injection + response unwrap**

```ts
if (resp.data.code !== 'SUCCESS') throw new Error(resp.data.message)
```

- [ ] **Step 3: Implement auth API and auth store (`login`, `logout`, `fetchMe`)**

```ts
await loginApi(payload)
token.value = resp.token
```

- [ ] **Step 4: Add route guard for protected routes**

```ts
if (!authStore.isAuthenticated && to.name !== 'login') return { name: 'login' }
```

- [ ] **Step 5: Implement login page with validation and submit lock**

Run: `cd easyarchive-ui && npm run build`  
Expected: build succeeds

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui/src
git commit -m "feat(ui): add unified http client and auth route guard"
```

### Task 3: Backend Dashboard Aggregation API and CORS Safety

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/DashboardController.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/DashboardService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/DashboardServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/SecurityConfig.java`

- [ ] **Step 1: Add dashboard service contract for status counts and lists**
- [ ] **Step 2: Implement mapper queries for task status distribution / recent tasks / failed tasks**
- [ ] **Step 3: Expose `GET /api/v1/dashboard/overview` returning one payload**
- [ ] **Step 4: Add minimal CORS config for local UI dev origin**
- [ ] **Step 5: Verify starter compile**

Run: `mvn -pl easyarchive-starter -am test -DskipTests`  
Expected: compile succeeds

- [ ] **Step 6: Commit**

```bash
git add easyarchive-starter
git commit -m "feat(starter): add dashboard overview api for ui console"
```

### Task 4: Datasource and User Management Pages

**Files:**
- Create: `easyarchive-ui/src/api/datasource.ts`
- Create: `easyarchive-ui/src/api/user.ts`
- Create: `easyarchive-ui/src/views/DatasourceView.vue`
- Create: `easyarchive-ui/src/views/UserView.vue`
- Create: `easyarchive-ui/src/components/DatasourceFormDialog.vue`
- Create: `easyarchive-ui/src/components/UserFormDialog.vue`
- Create: `easyarchive-ui/src/utils/dictionaries.ts`

- [ ] **Step 1: Add typed API modules for datasource and user list/create/update/status (datasource includes connection test)**
- [ ] **Step 2: Implement datasource list page + create/edit/status/test connection**
- [ ] **Step 3: Implement user list page + create/edit/status**
- [ ] **Step 4: Centralize status text/tag dictionaries**
- [ ] **Step 5: Verify build**

Run: `cd easyarchive-ui && npm run build`  
Expected: build succeeds

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui/src
git commit -m "feat(ui): implement datasource and user management pages"
```

### Task 5: Task List, Detail, Logs, and Cancel Flow

**Files:**
- Create: `easyarchive-ui/src/api/task.ts`
- Create: `easyarchive-ui/src/views/TaskListView.vue`
- Create: `easyarchive-ui/src/views/TaskDetailView.vue`
- Create: `easyarchive-ui/src/components/TaskStatusTag.vue`
- Create: `easyarchive-ui/src/utils/polling.ts`

- [ ] **Step 1: Add typed task APIs (`tasks`, `task detail`, `task logs`, `cancel`)**
- [ ] **Step 2: Build task list with pagination/filter/manual refresh**
- [ ] **Step 3: Build task detail with logs panel and cancel action**
- [ ] **Step 4: Add controlled polling utility and route lifecycle cleanup**
- [ ] **Step 5: Verify build**

Run: `cd easyarchive-ui && npm run build`  
Expected: build succeeds

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui/src
git commit -m "feat(ui): implement archive tasks list detail logs and cancel flow"
```

### Task 6: Dashboard View, App Integration, and Final Verification

**Files:**
- Create: `easyarchive-ui/src/api/dashboard.ts`
- Create: `easyarchive-ui/src/views/DashboardView.vue`
- Modify: `easyarchive-ui/src/router/index.ts`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/styles/theme.css`
- Modify: `pom.xml` (add `easyarchive-ui` module entry if needed as docs-only module tracking)
- Modify: `README.md`

- [ ] **Step 1: Build dashboard view with overview cards + recent/failed task tables**
- [ ] **Step 2: Wire routes and navigation for all pages**
- [ ] **Step 3: Polish responsive layout + stable loading/empty/error states**
- [ ] **Step 4: Document frontend startup and backend integration in README**
- [ ] **Step 5: End-to-end verification commands**

Run: `cd easyarchive-ui && npm run build`  
Expected: build succeeds  
Run: `mvn -pl easyarchive-starter -am test -DskipTests`  
Expected: compile succeeds

- [ ] **Step 6: Commit**

```bash
git add easyarchive-ui easyarchive-starter README.md pom.xml
git commit -m "feat(ui): deliver easyarchive operations console v1"
```
