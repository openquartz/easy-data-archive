# EasyArchive Full Test Report

Date: 2026-06-03
Tester: Codex
Workspace: `/Users/jackxu/Documents/Code/local/easy-archive-master`

## Remediation Update

The original defects documented below were reworked in the same session.

- Removed the stray `setSchemaName(...)` reflection call from `ArchiveGroupExecutionServiceImplTest`
- Unified user-status semantics to `0 = enabled`, `1 = disabled` in the UI
- Replaced the authenticated-shell login entry with account context plus logout action
- Fixed logout flow so it returns to `/login`

Post-fix verification:

- `mvn test` passed with 70 tests, 0 failures, 0 errors
- `npm run build` passed
- `npm run test:auth-contract` passed
- `npm run test:user-status-contract` passed
- `npm run smoke` passed
- Browser regression confirmed:
  - admin is shown as enabled
  - test user `qa_775704` is shown as disabled
  - logout redirects to `/login?redirect=/users`

## Scope

- Backend automated verification for `easyarchive-common`, `easyarchive-core`, `easyarchive-starter`
- Frontend build and contract/smoke verification for `easyarchive-ui`
- Browser-based end-to-end smoke validation against:
  - UI: `http://127.0.0.1:5173`
  - API: `http://127.0.0.1:8080`
- Real write-path verification through the user-management UI

Note: Per request, intermediate test data was not removed. A test user was created:

- `qa_775704` / `Qa123456`

## Environment

- Java: `11.0.31`
- Maven: `3.9.16`
- Node/NPM: repository-local install already available
- MySQL port `3306`: reachable
- Existing backend service on port `8080`: reachable and healthy
- Existing uncommitted changes were present before testing; they were not modified

## Commands Executed

### Backend

```bash
mvn test
mvn -pl easyarchive-starter -am -DskipTests package
curl -i http://127.0.0.1:8080/actuator/health
curl -i http://127.0.0.1:8080/api/v1/auth/me
curl -X POST http://127.0.0.1:8080/api/v1/auth/login ...
curl http://127.0.0.1:8080/api/v1/users ...
```

### Frontend

```bash
cd easyarchive-ui
npm run build
npm run test:auth-contract
npm run smoke
npm run dev -- --host 127.0.0.1 --port 5173
```

## Automated Results

### Passed

- `npm run build`
- `npm run test:auth-contract`
- `npm run smoke`
- `mvn -pl easyarchive-starter -am -DskipTests package`
- `GET /actuator/health` returned `200` with `{"status":"UP"}`
- `POST /api/v1/auth/login` succeeded for `admin/password`

### Failed

- `mvn test`

Failure summary:

- `easyarchive-starter` test suite failed
- 68 tests executed, 1 error, 0 assertion failures
- Failing test:
  - `ArchiveGroupExecutionServiceImplTest.shouldCreateWaitingTaskAndDispatch`

Root symptom:

- test helper calls `setSchemaName(...)`
- actual `ArchiveConnection` entity does not expose that setter
- failure type: `NoSuchMethodException`

## Browser-Based Functional Testing

### Verified flows

1. Open login page
2. Validate empty-form feedback
3. Log in with `admin/password`
4. Redirect to dashboard after login
5. Navigate to:
   - Dashboard
   - Datasources
   - Archive Groups
   - Tasks
   - Users
6. Create a user through the UI
7. Confirm created row appears in the user list
8. Attempt login with created user credentials

### Observed behavior

- Login page rendered correctly
- Route guard redirected authenticated access from `/login` back to `/dashboard`
- Dashboard, datasources, archive groups, tasks, and users pages all rendered without fatal UI breakage
- Empty states rendered correctly on datasource/group/task pages
- User creation succeeded visually and persisted to backend
- Newly created user could not log in although UI marked it as enabled

## Findings

### P1: User status semantics are inverted between frontend and backend

Evidence:

- Backend authentication treats `status != 0` as disabled in [AuthServiceImpl.java](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/AuthServiceImpl.java:35)
- Frontend maps user `0 -> disabled`, `1 -> enabled` in [dictionaries.ts](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/utils/dictionaries.ts:13)
- User form defaults new users to `status: 1` and labels that option as enabled in [UserFormDialog.vue](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/components/UserFormDialog.vue:24) and [UserFormDialog.vue](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/components/UserFormDialog.vue:129)
- Created user `qa_775704` was shown in UI as `已启用`, but API returned `"status":1` and login returned `401` with `"用户已禁用"`

Impact:

- Any newly created user is likely unusable for authentication when left at the default UI selection
- Existing enabled users appear disabled in the UI, which can mislead operators into toggling the wrong state

Recommendation:

- Unify user status semantics across backend DTOs, frontend dictionaries, form defaults, and action labels
- Add an end-to-end regression test: create enabled user -> login succeeds

### P1: Backend full test suite is not green

Evidence:

- Test helper calls `setSchemaName` in [ArchiveGroupExecutionServiceImplTest.java](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java:168)
- `ArchiveConnection` has no `schemaName` field or setter in [ArchiveConnection.java](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-core/src/main/java/com/openquartz/easyarchive/core/connection/entity/ArchiveConnection.java:14)

Impact:

- CI confidence is reduced because repository-level `mvn test` currently fails
- The failure is in a recently changed area around archive group execution and datasource modeling

Recommendation:

- Align the test fixture with the actual datasource model, or restore the missing field if the entity contract was unintentionally narrowed
- Keep `mvn test` green as a merge gate

### P2: Authenticated shell still exposes a “登录” navigation entry

Evidence:

- Sidebar always renders a login link in [AppLayout.vue](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/layouts/AppLayout.vue:18)
- After login, the shell still shows “登录” even though `/login` immediately redirects back to dashboard

Impact:

- Confusing navigation for operators
- Wastes space and implies an available action that does not actually behave like a login/logout control

Recommendation:

- Replace the login link with user identity plus logout action when authenticated
- Hide the login route entry from the authenticated shell

### P3: Login validation feedback is sequential rather than complete

Evidence:

- Submitting an empty login form surfaced only the username-required error first during browser testing

Impact:

- Users may need multiple submit attempts to discover all required fields

Recommendation:

- Show inline validation for both fields or surface all blocking messages at once

## Additional Risks

- Packaging logs include javadoc plugin `NullPointerException` output during `package`, although the final build still reported success. This should be reviewed before relying on release packaging as a documentation artifact path.
- The application could not be started on port `8080` from the packaged jar during this session because another Java process was already bound to that port. This was not a product defect because the resident service responded correctly.

## Experience Review

### Strengths

- UI loads quickly and key routes are stable
- Route guard behavior for authenticated users works
- Forms and empty states are readable
- Frontend build and smoke tooling are lightweight and fast

### Improvement Opportunities

- Add visible logout and current-user context in the shell
- Distinguish “no data yet” from “failed to load” more explicitly on list pages
- Add seeded demo data for datasources/archive groups/tasks in local environments so browser smoke tests cover more than empty states
- Add stronger success/error toasts around status toggles and writes
- Avoid exposing password hashes in user list API responses

## Recommended Next Actions

1. Fix the user status semantic mismatch first; it is a real functional defect affecting account usability.
2. Repair the failing backend test and re-run `mvn test`.
3. Add an end-to-end regression script covering login, user creation, and re-login with the created account.
4. Add logout UX and remove the authenticated “登录” nav item.
5. Harden test data fixtures so archive group and task pages can be exercised with non-empty data in local QA.

## Continuation: Datasource, Group, Item, Task Coverage

Follow-up test date: 2026-06-03

### Additional setup performed during testing

- Created datasource records:
  - `QA_SRC_9844`
  - `QA_DST_9844`
- Created archive group:
  - `QA_GROUP_9844`
- Created archive items for that group:
  - one `ID` item on `sys_user`
  - one `TIME` item on `sys_user`
- Triggered one archive execution task:
  - task `1`
- Added missing database objects in the local `openquartz` schema to continue testing:
  - `ea_archive_group_item_by_id`
  - `ea_archive_group_item_by_time`
  - missing columns on `ea_archive_task_log` from the expected V2 migration contract

### Additional verified behavior

- Datasource create API works
- Datasource status toggle API works
- Archive group create API works when `groupPath`, `groupLevel`, and `ownerUserId` are explicitly provided
- Archive group status toggle API works
- ID archive item create/list/status APIs work
- TIME archive item create/list/status APIs work
- Triggering archive group execution works
- Task list API works
- Task detail API works
- Task status filter works
- Cancelling a completed task correctly returns `INVALID_STATUS`
- Task logs API works after the local schema is brought up to the expected structure

### New findings from continuation

#### P1: Datasource connection test always returns false

Evidence:

- `POST /api/v1/archive/datasources/test` returned `false` for both a valid local MySQL connection and an intentionally invalid one
- In [DatasourceConnectionTester.java](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/support/DatasourceConnectionTester.java:24), the implementation ignores the submitted password and uses the literal string `"******"`

Impact:

- The datasource “test connection” feature is not trustworthy
- Operators cannot distinguish valid from invalid datasource configuration

Recommendation:

- Use the real submitted password (or decrypted value) during connection tests
- Add positive and negative automated contract tests for `/archive/datasources/test`

#### P1: Archive group creation is broken for the current UI payload shape

Evidence:

- Creating a group with the UI-equivalent payload failed with MySQL error `Column 'group_level' cannot be null`
- The mapper insert in [ArchiveGroupMapper.xml](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml:41) explicitly writes `group_path` and `group_level`, so database defaults are bypassed when the frontend omits them
- The current form in [ArchiveGroupFormDialog.vue](/Users/jackxu/Documents/Code/local/easy-archive-master/easyarchive-ui/src/components/ArchiveGroupFormDialog.vue:1) does not collect those fields

Impact:

- Archive group creation from the current frontend flow is effectively broken against a strict schema

Recommendation:

- Set backend defaults for `groupPath`, `groupLevel`, and `ownerUserId` in the service layer
- Or make the mapper conditional so database defaults can apply
- Add an automated controller/service test for UI-minimal create payloads

#### P1: Local runtime schema is behind application expectations

Evidence:

- Group item APIs initially failed because `ea_archive_group_item_by_id` and `ea_archive_group_item_by_time` did not exist
- Task log API initially failed because `ea_archive_task_log` lacked `updated_time`, `creator_id`, `updater_id`, and `deleted`

Impact:

- The “归档分组明细” and “任务日志” parts of the product are not runnable on a partially migrated environment

Recommendation:

- Ensure Flyway migrations are actually applied in local/dev startup
- Add a startup health check or migration verification endpoint so schema drift is visible before users hit the UI
