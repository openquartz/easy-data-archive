# In-App Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reusable platform in-app notification framework, first wired to archive-group completion events, with configurable recipients, 7-day retention, per-user read state, and a top-right bell dropdown.

**Architecture:** Reuse the existing archive task terminal event as the first trigger, persist one platform-generic notification snapshot plus one recipient inbox row per user, enforce 7-day retention with a scheduled cleanup task, and surface the inbox via dedicated API endpoints and a new UI bell component. Keep external webhook notifications and in-app notifications parallel but separate so the new feature does not distort the existing `notifyChannel` model.

**Tech Stack:** Spring Boot 2.3.2, MyBatis XML mappers, Flyway SQL migrations, Vue 3 + Pinia + Vue Router + TypeScript, Node test runner, JUnit 5 + Mockito

---

### Task 1: Add schema and backend notification membership models

**Files:**
- Create: `easyarchive-starter/src/main/resources/db/migration/V9__add_in_app_notifications.sql`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/ArchiveGroupNotificationUser.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/InAppNotification.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/InAppNotificationRecipient.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationBizTypeEnum.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationReadStatusEnum.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationCategoryEnum.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationLevelEnum.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupNotificationUserMapper.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/InAppNotificationMapper.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/InAppNotificationRecipientMapper.java`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupNotificationUserMapper.xml`
- Create: `easyarchive-starter/src/main/resources/mapper/InAppNotificationMapper.xml`
- Create: `easyarchive-starter/src/main/resources/mapper/InAppNotificationRecipientMapper.xml`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/db/MigrationVersioningTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/mapper/MapperXmlParsingTest.java`

- [ ] **Step 1: Write the failing migration/version tests**

```java
@Test
void shouldContainVersion9Migration() {
    List<String> versions = migrationVersions();
    assertTrue(versions.contains("V9__add_in_app_notifications.sql"));
}
```

```java
@Test
void shouldParseInAppNotificationMappers() {
    assertDoesNotThrow(() -> parseMapper("mapper/InAppNotificationMapper.xml"));
    assertDoesNotThrow(() -> parseMapper("mapper/InAppNotificationRecipientMapper.xml"));
    assertDoesNotThrow(() -> parseMapper("mapper/ArchiveGroupNotificationUserMapper.xml"));
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=MigrationVersioningTest,MapperXmlParsingTest
```

Expected: FAIL because `V9__add_in_app_notifications.sql` and the new mapper XML files do not exist yet.

- [ ] **Step 3: Add the migration and persistence model**

```sql
ALTER TABLE `ea_archive_group`
    ADD COLUMN `in_app_notify_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '0-关闭站内通知 1-开启站内通知';

CREATE TABLE `ea_archive_group_notification_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `group_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `created_by` VARCHAR(64) DEFAULT NULL,
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_by` VARCHAR(64) DEFAULT NULL,
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_user` (`group_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) COMMENT='归档分组站内通知成员';

CREATE TABLE `ea_in_app_notification` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `biz_type` VARCHAR(32) NOT NULL,
    `biz_id` BIGINT NOT NULL,
    `category` VARCHAR(32) NOT NULL,
    `level` VARCHAR(16) NOT NULL,
    `group_id` BIGINT DEFAULT NULL,
    `group_name` VARCHAR(128) DEFAULT NULL,
    `task_id` BIGINT DEFAULT NULL,
    `task_status` VARCHAR(16) DEFAULT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content_summary` VARCHAR(500) NOT NULL,
    `payload_json` TEXT NOT NULL,
    `source_time` DATETIME NOT NULL,
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_biz_type_biz_id_status` (`biz_type`, `biz_id`, `task_status`)
) COMMENT='平台站内通知主表';
```

```java
public class ArchiveGroupNotificationUser {
    private Long id;
    private Long groupId;
    private Long userId;
    private String createdBy;
    private Date createdTime;
    private String updatedBy;
    private Date updatedTime;
}
```

```java
public interface InAppNotificationRecipientMapper {
    int insertBatch(@Param("items") List<InAppNotificationRecipient> items);
    int countUnreadByUserId(@Param("userId") Long userId);
}
```

- [ ] **Step 4: Run tests to verify the schema layer passes**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=MigrationVersioningTest,MapperXmlParsingTest
```

Expected: PASS with the new migration visible and all new MyBatis XML files parseable.

- [ ] **Step 5: Commit the schema/model slice**

```bash
git add easyarchive-starter/src/main/resources/db/migration/V9__add_in_app_notifications.sql \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/ArchiveGroupNotificationUser.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/InAppNotification.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/entity/InAppNotificationRecipient.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationBizTypeEnum.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationReadStatusEnum.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationCategoryEnum.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/enums/InAppNotificationLevelEnum.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupNotificationUserMapper.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/InAppNotificationMapper.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/InAppNotificationRecipientMapper.java \
  easyarchive-starter/src/main/resources/mapper/ArchiveGroupNotificationUserMapper.xml \
  easyarchive-starter/src/main/resources/mapper/InAppNotificationMapper.xml \
  easyarchive-starter/src/main/resources/mapper/InAppNotificationRecipientMapper.xml \
  easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupView.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/db/MigrationVersioningTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/mapper/MapperXmlParsingTest.java
git commit -m "feat: add in-app notification schema"
```

### Task 2: Persist terminal notifications from archive task events

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/inapp/ArchiveInAppNotificationListener.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/inapp/ArchiveInAppNotificationService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/inapp/InAppNotificationMessageBuilder.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/task/InAppNotificationCleanupTask.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/InAppNotificationPayload.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/inapp/ArchiveInAppNotificationServiceTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/inapp/ArchiveInAppNotificationListenerTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/inapp/InAppNotificationMessageBuilderTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/task/InAppNotificationCleanupTaskTest.java`

- [ ] **Step 1: Write failing service and listener tests**

```java
@Test
void shouldCreateOneNotificationAndInboxRowsForConfiguredRecipients() {
    when(groupMapper.selectById(7L)).thenReturn(groupWithInAppNotifyEnabled());
    when(memberMapper.selectUserIdsByGroupId(7L)).thenReturn(Arrays.asList(11L, 12L));

    service.handleTaskTerminated(task(99L, 7L, ArchiveTaskStatusEnum.SUCCESS.getCode()));

    verify(notificationMapper).insert(any(InAppNotification.class));
    verify(recipientMapper).insertBatch(argThat(items -> items.size() == 2));
}
```

```java
@Test
void shouldIgnoreDuplicateTaskTerminalEvent() {
    when(notificationMapper.insert(any(InAppNotification.class))).thenThrow(new DuplicateKeyException("dup"));
    assertDoesNotThrow(() -> service.handleTaskTerminated(task(99L, 7L, ArchiveTaskStatusEnum.SUCCESS.getCode())));
    verify(recipientMapper, never()).insertBatch(anyList());
}
```

```java
@Test
void shouldCleanupNotificationsOlderThanSevenDays() {
    cleanupTask.cleanup();
    verify(recipientMapper).deleteReadModelOlderThan(7);
    verify(notificationMapper).deleteOrphansOlderThan(7);
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveInAppNotificationServiceTest,ArchiveInAppNotificationListenerTest,InAppNotificationMessageBuilderTest,InAppNotificationCleanupTaskTest
```

Expected: FAIL because the in-app notification service, listener, and payload builder do not exist yet.

- [ ] **Step 3: Implement the terminal notification pipeline**

```java
public void handleTaskTerminated(ArchiveGroupExecuteTask task) {
    if (!ArchiveTaskStatusEnum.isFinished(task.getExecuteStatus())) {
        return;
    }
    ArchiveGroup group = groupMapper.selectById(task.getGroupId());
    if (group == null || !isInAppNotifyEnabled(group.getInAppNotifyEnabled())) {
        return;
    }
    List<Long> recipientIds = filterValidRecipients(memberMapper.selectUserIdsByGroupId(group.getId()));
    if (recipientIds.isEmpty()) {
        log.warn("Skip in-app notification because no valid recipients. groupId={}, taskId={}", group.getId(), task.getId());
        return;
    }
    InAppNotification notification = messageBuilder.build(group, task, fetchTaskSummary(task.getId()));
    tryInsertNotification(notification);
    recipientMapper.insertBatch(buildRecipients(notification.getId(), recipientIds));
}
```

```java
private void tryInsertNotification(InAppNotification notification) {
    try {
        notificationMapper.insert(notification);
    } catch (DuplicateKeyException ex) {
        log.info("In-app notification already exists. taskId={}, status={}", notification.getTaskId(), notification.getTaskStatus());
        throw ex;
    }
}
```

```java
@Scheduled(cron = "0 10 3 * * ?")
public void cleanup() {
    recipientMapper.deleteReadModelOlderThan(7);
    notificationMapper.deleteOrphansOlderThan(7);
}
```

- [ ] **Step 4: Run tests to verify the pipeline passes**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveInAppNotificationServiceTest,ArchiveInAppNotificationListenerTest,InAppNotificationMessageBuilderTest
```

Expected: PASS with success, failure, cancel, disabled-group, empty-recipient, duplicate-event, and 7-day cleanup cases covered.

- [ ] **Step 5: Commit the notification generation slice**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/notification/inapp \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/task/InAppNotificationCleanupTask.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/InAppNotificationPayload.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/config/EasyArchiveAutoConfiguration.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/notification/inapp \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/task/InAppNotificationCleanupTaskTest.java
git commit -m "feat: persist archive in-app notifications"
```

### Task 3: Expose recipient config and bell inbox APIs

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/InAppNotificationController.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/InAppNotificationQueryService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/InAppNotificationQueryServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/InAppNotificationListItem.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/InAppNotificationUnreadCountView.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupNotificationMemberConfig.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/InAppNotificationControllerContractTest.java`
- Modify: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/InAppNotificationQueryServiceImplTest.java`

- [ ] **Step 1: Write failing controller and service tests**

```java
@Test
void shouldReturnUnreadCountForCurrentUser() throws Exception {
    when(queryService.getUnreadCount()).thenReturn(new InAppNotificationUnreadCountView(3));

    mockMvc.perform(get("/api/v1/in-app-notifications/unread-count").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.unreadCount").value(3));
}
```

```java
@Test
void shouldRejectEnabledMemberConfigWithoutRecipients() {
    StarterManageException ex = assertThrows(
            StarterManageException.class,
            () -> service.saveNotificationMembers(7L, request(true, Collections.emptyList()))
    );
    assertEquals(StarterErrorCode.ARCHIVE_GROUP_NOTIFICATION_RECIPIENT_REQUIRED, ex.getErrorCode());
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=InAppNotificationControllerContractTest,ArchiveGroupControllerContractTest,InAppNotificationQueryServiceImplTest,ArchiveGroupServiceImplTest
```

Expected: FAIL because the inbox controller, query service, and group member config methods are missing.

- [ ] **Step 3: Implement API endpoints and service methods**

```java
@GetMapping("/unread-count")
public ApiResponse<InAppNotificationUnreadCountView> unreadCount() {
    return ApiResponse.success(queryService.getUnreadCount());
}

@PostMapping("/{id}/read")
public ApiResponse<?> markRead(@PathVariable Long id) {
    queryService.markRead(id);
    return ApiResponse.success();
}
```

```java
public ArchiveGroupNotificationMemberConfig saveNotificationMembers(Long groupId, ArchiveGroupNotificationMemberConfig request) {
    dataPermissionService.assertAdmin();
    ArchiveGroup group = ensureExists(groupId);
    if (EnableStatusEnum.isEnabled(request.getInAppNotifyEnabled()) && request.getRecipientUserIds().isEmpty()) {
        throw new StarterManageException(StarterErrorCode.ARCHIVE_GROUP_NOTIFICATION_RECIPIENT_REQUIRED);
    }
    groupMapper.updateInAppNotifyEnabled(groupId, request.getInAppNotifyEnabled());
    memberMapper.replaceGroupMembers(groupId, dedupeValidUsers(request.getRecipientUserIds()));
    return loadNotificationMembers(groupId);
}
```

```java
public class InAppNotificationListItem {
    private Long notificationId;
    private String bizType;
    private String category;
    private String level;
    private String title;
    private String summary;
    private Integer readStatus;
    private Date createdTime;
    private Long groupId;
    private Long taskId;
}
```

- [ ] **Step 4: Run tests to verify the API slice passes**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=InAppNotificationControllerContractTest,ArchiveGroupControllerContractTest,InAppNotificationQueryServiceImplTest,ArchiveGroupServiceImplTest
```

Expected: PASS with unread-count, inbox list, mark-read, mark-all-read, recipient-config load, and recipient-config save covered.

- [ ] **Step 5: Commit the API slice**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/InAppNotificationController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/InAppNotificationQueryService.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/InAppNotificationQueryServiceImpl.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/InAppNotificationListItem.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/InAppNotificationUnreadCountView.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupNotificationMemberConfig.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java \
  easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/InAppNotificationControllerContractTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/InAppNotificationQueryServiceImplTest.java \
  easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java
git commit -m "feat: add in-app notification APIs"
```

### Task 4: Add archive-group notification-member configuration to the UI

**Files:**
- Modify: `easyarchive-ui/src/api/archiveGroup.ts`
- Modify: `easyarchive-ui/src/api/user.ts`
- Create: `easyarchive-ui/src/utils/archiveGroupNotification.ts`
- Modify: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- Modify: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Create: `easyarchive-ui/tests/archive-group-notification-config.test.ts`

- [ ] **Step 1: Write the failing UI tests for form serialization and validation**

```ts
test("requires at least one recipient when in-app notify is enabled", () => {
  const form = createArchiveGroupNotificationForm({
    inAppNotifyEnabled: 1,
    inAppNotificationRecipientUserIds: []
  });

  assert.equal(validateArchiveGroupNotificationForm(form), "inAppRecipientRequired");
});
```

```ts
test("normalizes recipient ids before submit", () => {
  const payload = buildArchiveGroupPayload({
    inAppNotifyEnabled: 1,
    inAppNotificationRecipientUserIds: [11, 11, 12]
  });

  assert.deepEqual(payload.inAppNotificationRecipientUserIds, [11, 12]);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd easyarchive-ui && npm test -- archive-group-notification-config.test.ts
```

Expected: FAIL because the notification-recipient form utility and fields do not exist.

- [ ] **Step 3: Implement the form payload fields and member picker UI**

```ts
export interface ArchiveGroupPayload {
  inAppNotifyEnabled?: number;
  inAppNotificationRecipientUserIds?: number[];
}
```

```vue
<label>
  {{ t("archiveGroup.form.inAppNotifyEnabled") }}
  <select v-model.number="form.inAppNotifyEnabled" :disabled="submitting">
    <option :value="0">{{ t("common.no") }}</option>
    <option :value="1">{{ t("common.yes") }}</option>
  </select>
</label>

<fieldset class="full-width" v-if="form.inAppNotifyEnabled === 1">
  <legend>{{ t("archiveGroup.form.inAppRecipients") }}</legend>
  <label v-for="user in candidateUsers" :key="user.id" class="checkbox-option">
    <input v-model="form.inAppNotificationRecipientUserIds" :value="user.id" type="checkbox" />
    <span>{{ user.realName || user.username }} ({{ user.username }})</span>
  </label>
</fieldset>
```

- [ ] **Step 4: Run UI tests and build**

Run:

```bash
cd easyarchive-ui && npm test -- archive-group-notification-config.test.ts
cd easyarchive-ui && npm run build
```

Expected: PASS with correct field normalization and no TypeScript build errors.

- [ ] **Step 5: Commit the group-config UI slice**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts \
  easyarchive-ui/src/api/user.ts \
  easyarchive-ui/src/utils/archiveGroupNotification.ts \
  easyarchive-ui/src/components/ArchiveGroupFormDialog.vue \
  easyarchive-ui/src/views/ArchiveGroupView.vue \
  easyarchive-ui/src/i18n/messages.ts \
  easyarchive-ui/tests/archive-group-notification-config.test.ts
git commit -m "feat: configure archive group in-app recipients"
```

### Task 5: Add the top-right bell inbox UI and connect it to the new APIs

**Files:**
- Create: `easyarchive-ui/src/api/inAppNotification.ts`
- Create: `easyarchive-ui/src/stores/inAppNotification.ts`
- Create: `easyarchive-ui/src/components/InAppNotificationBell.vue`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/router/index.ts`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Create: `easyarchive-ui/tests/in-app-notification-bell.test.ts`

- [ ] **Step 1: Write the failing bell store/component tests**

```ts
test("shows unread badge capped at 9+", async () => {
  const store = createInAppNotificationStore();
  store.unreadCount = 12;
  assert.equal(store.badgeLabel, "9+");
});
```

```ts
test("marks a notification as read when opening task detail", async () => {
  const api = createFakeNotificationApi();
  await handleNotificationClick(api, { notificationId: 1, taskId: 88, groupId: 7, readStatus: 0 });
  assert.deepEqual(api.calls, ["read:1", "task:88"]);
});
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd easyarchive-ui && npm test -- in-app-notification-bell.test.ts
```

Expected: FAIL because the bell store, bell component, and in-app notification API client do not exist yet.

- [ ] **Step 3: Implement the store, API client, and bell component**

```ts
export const useInAppNotificationStore = defineStore("inAppNotification", {
  state: () => ({
    unreadCount: 0,
    items: [] as NotificationListItem[],
    loading: false
  }),
  getters: {
    badgeLabel: (state) => (state.unreadCount > 9 ? "9+" : String(state.unreadCount || ""))
  }
});
```

```vue
<button class="notification-bell" @click="toggleOpen">
  <span class="notification-bell__icon">🔔</span>
  <span v-if="store.unreadCount > 0" class="notification-bell__badge">{{ store.badgeLabel }}</span>
</button>
```

```ts
export function getUnreadCountApi(): Promise<NotificationUnreadCount> {
  return http.get<NotificationUnreadCount>("/in-app-notifications/unread-count");
}
```

- [ ] **Step 4: Run UI tests and production build**

Run:

```bash
cd easyarchive-ui && npm test -- in-app-notification-bell.test.ts
cd easyarchive-ui && npm run build
```

Expected: PASS with unread badge, empty state, mark-read, mark-all-read, and route-jump behavior covered.

- [ ] **Step 5: Commit the bell UI slice**

```bash
git add easyarchive-ui/src/api/inAppNotification.ts \
  easyarchive-ui/src/stores/inAppNotification.ts \
  easyarchive-ui/src/components/InAppNotificationBell.vue \
  easyarchive-ui/src/layouts/AppLayout.vue \
  easyarchive-ui/src/router/index.ts \
  easyarchive-ui/src/i18n/messages.ts \
  easyarchive-ui/tests/in-app-notification-bell.test.ts
git commit -m "feat: add in-app notification bell"
```

### Task 6: Run end-to-end verification across backend and frontend

**Files:**
- Modify: `docs/superpowers/specs/2026-06-05-in-app-notification-design.md`
- Create: `docs/reports/2026-06-05-in-app-notification-verification.md`

- [ ] **Step 1: Update the spec with implementation notes if names changed**

```md
## Implementation Notes

- Final API path: `/api/v1/in-app-notifications`
- Final group config field names: `inAppNotifyEnabled`, `inAppNotificationRecipientUserIds`
- Final bell behavior: click marks one item read before route navigation
- Final retention policy: in-app notifications are retained for 7 days only
- Final generic fields: `bizType`, `category`, `level`, `payloadJson`
```

- [ ] **Step 2: Run the backend verification suite**

Run:

```bash
mvn test -pl easyarchive-starter \
  -Dtest=ArchiveInAppNotificationServiceTest,ArchiveInAppNotificationListenerTest,InAppNotificationMessageBuilderTest,InAppNotificationControllerContractTest,ArchiveGroupControllerContractTest,ArchiveGroupServiceImplTest,MigrationVersioningTest,MapperXmlParsingTest
```

Expected: PASS.

- [ ] **Step 3: Run the frontend verification suite**

Run:

```bash
cd easyarchive-ui && npm test -- archive-group-notification-config.test.ts in-app-notification-bell.test.ts
cd easyarchive-ui && npm run build
```

Expected: PASS.

- [ ] **Step 4: Write the verification report**

```md
# In-App Notification Verification

- Backend targeted tests: PASS
- Frontend targeted tests: PASS
- Production build: PASS
- Manual checks completed:
  - enabled group requires recipients
  - terminal event creates inbox rows
  - cleanup removes notifications older than 7 days
  - bell shows unread badge
  - click marks item read and opens task/group detail
```

- [ ] **Step 5: Commit the verification and documentation updates**

```bash
git add docs/superpowers/specs/2026-06-05-in-app-notification-design.md \
  docs/reports/2026-06-05-in-app-notification-verification.md
git commit -m "docs: record in-app notification verification"
```

## Self-Review

- Spec coverage:
  - terminal status scope is covered by Task 2
  - explicit group recipient membership is covered by Tasks 1, 3, and 4
  - per-user inbox and read state is covered by Tasks 1, 2, and 3
  - top-right bell interaction is covered by Task 5
  - tests and rollout verification are covered by Task 6
- Placeholder scan:
  - no `TBD`, `TODO`, or “implement later” placeholders remain
  - every code-writing step contains concrete file targets and example code
- Type consistency:
  - backend uses `inAppNotifyEnabled` and `inAppNotificationRecipientUserIds`
  - frontend mirrors the same names in payload fields and tests

Plan complete and saved to `docs/superpowers/plans/2026-06-05-in-app-notification-implementation.md`. Two execution options:

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
