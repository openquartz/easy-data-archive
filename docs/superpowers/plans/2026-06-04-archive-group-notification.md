# Archive Group Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add per-group completion notifications that send Feishu or WeCom webhook messages for success, failure, and cancellation terminal states.

**Architecture:** Extend archive group storage and APIs with a single notification configuration, then add a platform-side notification listener that reacts to `TaskEndEvent`, loads task/group/log data, builds a unified message, and sends it through a channel-specific webhook client. Keep notification failures isolated from archive execution and cover the behavior with service, listener, and controller/UI tests.

**Tech Stack:** Java 11, Spring Boot 2.3.2, MyBatis, JUnit 5, Mockito, Vue 3, TypeScript, Vite

---

## File Structure

- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroup.java`
- Modify: `easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql`
- Create: `easyarchive-starter/src/main/resources/db/migration/V6__add_archive_group_notification.sql`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/NotificationChannelEnum.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationListener.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/NotificationClient.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/NotificationMessageBuilder.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/model/*`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/support/FeishuNotificationClient.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/support/WeComNotificationClient.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveTaskLogMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveTaskLogMapper.xml`
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationServiceTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationListenerTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/NotificationMessageBuilderTest.java`

### Task 1: Extend Archive Group Persistence And Validation

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroup.java`
- Modify: `easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql`
- Create: `easyarchive-starter/src/main/resources/db/migration/V6__add_archive_group_notification.sql`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/NotificationChannelEnum.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Test: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`

- [ ] Write failing backend tests for valid notification config, invalid enabled-without-webhook config, and API response fields.
- [ ] Run focused Maven tests and confirm they fail for missing notification fields/validation.
- [ ] Implement entity, mapper XML, migration, enum, and service validation changes.
- [ ] Re-run focused Maven tests and confirm they pass.

### Task 2: Add Notification Listener, Service, And Message Builder

**Files:**
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveTaskLogMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveTaskLogMapper.xml`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationListener.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/NotificationClient.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/NotificationMessageBuilder.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/model/*`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/support/FeishuNotificationClient.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/support/WeComNotificationClient.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationServiceTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/ArchiveNotificationListenerTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/NotificationMessageBuilderTest.java`

- [ ] Write failing tests for listener terminal-state behavior, service skip rules, message rendering, and channel dispatch.
- [ ] Run focused Maven tests and confirm they fail for missing notification module classes/behavior.
- [ ] Implement notification models, builder, webhook clients, mapper query for task logs, service orchestration, and listener wiring.
- [ ] Re-run focused Maven tests and confirm they pass.

### Task 3: Add Group Form Notification Controls

**Files:**
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`

- [ ] Update TypeScript API types first so the form can model notification fields.
- [ ] Add failing form validation expectations mentally against current UI behavior, then implement the minimal form/watch/validation changes for enabled-without-channel/webhook and edit backfill.
- [ ] Run the frontend build to catch type/template regressions.

### Task 4: Full Verification

**Files:**
- Verify only

- [ ] Run targeted backend tests for archive group service, archive group controller, and notification package.
- [ ] Run the backend module test suite if the focused tests are green.
- [ ] Run the frontend build.
- [ ] Review `git diff` for unintended changes in the worktree before reporting completion.
