# ArchiveGroup Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build ArchiveGroup management end-to-end with split ID/TIME item tables mapped directly to `ArchiveGroupItemById` and `ArchiveGroupItemByTime`.

**Architecture:** `ea_archive_group` is the parent table. `ea_archive_group_item_by_id` and `ea_archive_group_item_by_time` are strategy-specific child tables loaded by a group-scoped `PlatformArchiveRuleLoader`, then executed by the existing `ArchiveGroupExecutor` and monitored through the existing task center.

**Tech Stack:** Java 11, Spring Boot 2.3.2, MyBatis XML mappers, JUnit 5, Mockito, Vue 3, Vue Router, TypeScript, Vite.

---

## File Structure

Backend core:

- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroup.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemById.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemByTime.java`

Backend starter schema:

- Modify: `easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql`
- Create: `easyarchive-starter/src/main/resources/db/migration/V3__archive_group_item_tables.sql`

Backend starter mappers:

- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupMapper.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByIdMapper.xml`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByTimeMapper.xml`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`

Backend starter services and support:

- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemSummary.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/rule/PlatformArchiveRuleLoader.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupItemByIdService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupItemByTimeService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupExecutionService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/support/ArchiveGroupTaskDispatcher.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java`

Backend starter controllers:

- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemController.java`

Backend tests:

- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/rule/PlatformArchiveRuleLoaderTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemControllerContractTest.java`

Frontend:

- Create: `easyarchive-ui/src/api/archiveGroup.ts`
- Create: `easyarchive-ui/src/api/archiveGroupItem.ts`
- Create: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Create: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- Create: `easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue`
- Create: `easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue`
- Modify: `easyarchive-ui/src/router/index.ts`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Modify: `easyarchive-ui/src/utils/dictionaries.ts`
- Modify: `easyarchive-ui/scripts/smoke-check.mjs`

---

### Task 1: Align Core Entities

**Files:**
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroup.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemById.java`
- Modify: `easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemByTime.java`

- [ ] **Step 1: Update `ArchiveGroup` fields**

Replace the current `ArchiveGroup` fields with table-aligned fields:

```java
private Long id;
private Long parentId;
private String groupCode;
private String groupName;
private String groupPath;
private Integer groupLevel;
private Long sourceDatasourceId;
private Long targetDatasourceId;
private Long ownerUserId;
private Integer enableStatus;
private String triggerMode;
private String remark;
```

- [ ] **Step 2: Keep item entities mapped to child table columns**

Confirm `ArchiveGroupItemById` contains these fields:

```java
private Long id;
private String sourceTable;
private String targetTable;
private Long groupId;
private Integer priority;
private String fetchSql;
private String deleteWhere;
private String startId = "0";
private String endId = String.valueOf(Long.MAX_VALUE);
private Integer stepCount = 1000;
private Integer stepRounds = 5000;
private Integer pauseMs;
private Integer enableClean;
private Integer enableWrite;
private Integer enableStatus;
private String idColumn = "ID";
```

Confirm `ArchiveGroupItemByTime` contains these fields:

```java
private Long id;
private String sourceTable;
private String targetTable;
private Long groupId;
private Integer priority;
private String fetchSql;
private String deleteWhere;
private Date startTime;
private Integer keepDay;
private Integer stepMinutes;
private Integer stepCount = 1000;
private Integer pauseMs;
private Integer enableClean;
private Integer enableWrite;
private Integer enableStatus;
private String idColumn = "ID";
```

- [ ] **Step 3: Compile core module**

Run:

```bash
mvn test -pl easyarchive-core
```

Expected: core module compiles and existing tests pass.

- [ ] **Step 4: Commit**

```bash
git add easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroup.java easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemById.java easyarchive-core/src/main/java/com/openquartz/easyarchive/core/rule/entity/ArchiveGroupItemByTime.java
git commit -m "feat: align archive group entities"
```

### Task 2: Add Split Item Tables and Remove Generic Rule Schema

**Files:**
- Modify: `easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql`
- Create: `easyarchive-starter/src/main/resources/db/migration/V3__archive_group_item_tables.sql`

- [ ] **Step 1: Remove generic rule tables from fresh schema**

In `V1__init_archive_platform.sql`, remove the `CREATE TABLE IF NOT EXISTS ea_archive_rule` block and the `CREATE TABLE IF NOT EXISTS ea_archive_rule_condition` block. Keep `ea_archive_group` unchanged.

- [ ] **Step 2: Add migration for upgrade path and child tables**

Create `V3__archive_group_item_tables.sql` with:

```sql
-- V3: Replace generic archive rule tables with concrete archive group item tables.

DROP TABLE IF EXISTS `ea_archive_rule_condition`;
DROP TABLE IF EXISTS `ea_archive_rule`;

CREATE TABLE IF NOT EXISTS `ea_archive_group_item_by_id` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '归档分组 ID',
    `source_table` VARCHAR(128) NOT NULL COMMENT '来源表',
    `target_table` VARCHAR(128) NOT NULL COMMENT '目标表',
    `priority` INT NOT NULL COMMENT '组内执行优先级',
    `fetch_sql` TEXT NOT NULL COMMENT '抓取 SQL',
    `delete_where` TEXT NULL COMMENT '删除保护条件',
    `start_id` VARCHAR(255) NOT NULL DEFAULT '0' COMMENT '起始 ID 表达式',
    `end_id` VARCHAR(255) NOT NULL DEFAULT '9223372036854775807' COMMENT '结束 ID 表达式',
    `step_count` INT NOT NULL DEFAULT 1000 COMMENT '单批大小',
    `step_rounds` INT NOT NULL DEFAULT 5000 COMMENT 'ID 滚动窗口',
    `pause_ms` INT NULL COMMENT '批间停顿毫秒',
    `enable_clean` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用清理 1-不清理',
    `enable_write` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用写入 1-不写入',
    `enable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `id_column` VARCHAR(64) NOT NULL DEFAULT 'ID' COMMENT 'ID 字段名',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` VARCHAR(64) NULL COMMENT '创建人ID',
    `updater_id` VARCHAR(64) NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_status` (`group_id`, `enable_status`),
    INDEX `idx_group_priority_id` (`group_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按 ID 归档分组明细';

CREATE TABLE IF NOT EXISTS `ea_archive_group_item_by_time` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `group_id` BIGINT NOT NULL COMMENT '归档分组 ID',
    `source_table` VARCHAR(128) NOT NULL COMMENT '来源表',
    `target_table` VARCHAR(128) NOT NULL COMMENT '目标表',
    `priority` INT NOT NULL COMMENT '组内执行优先级',
    `fetch_sql` TEXT NOT NULL COMMENT '抓取 SQL',
    `delete_where` TEXT NULL COMMENT '删除保护条件',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `keep_day` INT NOT NULL COMMENT '保留天数',
    `step_minutes` INT NOT NULL COMMENT '时间滚动窗口分钟',
    `step_count` INT NOT NULL DEFAULT 1000 COMMENT '单批大小',
    `pause_ms` INT NULL COMMENT '批间停顿毫秒',
    `enable_clean` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用清理 1-不清理',
    `enable_write` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用写入 1-不写入',
    `enable_status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-启用 1-禁用',
    `id_column` VARCHAR(64) NOT NULL DEFAULT 'ID' COMMENT 'ID 字段名',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `creator_id` VARCHAR(64) NULL COMMENT '创建人ID',
    `updater_id` VARCHAR(64) NULL COMMENT '更新人ID',
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '删除标记',
    PRIMARY KEY (`id`),
    INDEX `idx_group_status` (`group_id`, `enable_status`),
    INDEX `idx_group_priority_time` (`group_id`, `priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按时间归档分组明细';
```

- [ ] **Step 3: Compile starter resources**

Run:

```bash
mvn test -pl easyarchive-starter -DskipTests
```

Expected: resources are copied and starter module compiles.

- [ ] **Step 4: Commit**

```bash
git add easyarchive-starter/src/main/resources/db/migration/V1__init_archive_platform.sql easyarchive-starter/src/main/resources/db/migration/V3__archive_group_item_tables.sql
git commit -m "feat: add archive group item tables"
```

### Task 3: Implement MyBatis Mappers

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupMapper.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByIdMapper.xml`
- Create: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByTimeMapper.xml`

- [ ] **Step 1: Create mapper interfaces**

`ArchiveGroupMapper`:

```java
@Mapper
public interface ArchiveGroupMapper {
    int insert(ArchiveGroup group);
    int update(ArchiveGroup group);
    int updateStatus(@Param("id") Long id, @Param("enableStatus") Integer enableStatus);
    int deleteById(@Param("id") Long id);
    ArchiveGroup selectById(@Param("id") Long id);
    ArchiveGroup selectByCode(@Param("groupCode") String groupCode);
    List<ArchiveGroup> selectList(@Param("enableStatus") Integer enableStatus);
}
```

`ArchiveGroupItemByIdMapper`:

```java
@Mapper
public interface ArchiveGroupItemByIdMapper {
    int insert(ArchiveGroupItemById item);
    int update(ArchiveGroupItemById item);
    int updateStatus(@Param("id") Long id, @Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);
    int deleteById(@Param("id") Long id, @Param("groupId") Long groupId);
    ArchiveGroupItemById selectById(@Param("id") Long id, @Param("groupId") Long groupId);
    List<ArchiveGroupItemById> selectByGroupId(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);
    int countEnabledByGroupId(@Param("groupId") Long groupId);
    int countPriority(@Param("groupId") Long groupId, @Param("priority") Integer priority, @Param("excludeId") Long excludeId);
}
```

`ArchiveGroupItemByTimeMapper`:

```java
@Mapper
public interface ArchiveGroupItemByTimeMapper {
    int insert(ArchiveGroupItemByTime item);
    int update(ArchiveGroupItemByTime item);
    int updateStatus(@Param("id") Long id, @Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);
    int deleteById(@Param("id") Long id, @Param("groupId") Long groupId);
    ArchiveGroupItemByTime selectById(@Param("id") Long id, @Param("groupId") Long groupId);
    List<ArchiveGroupItemByTime> selectByGroupId(@Param("groupId") Long groupId, @Param("enableStatus") Integer enableStatus);
    int countEnabledByGroupId(@Param("groupId") Long groupId);
    int countPriority(@Param("groupId") Long groupId, @Param("priority") Integer priority, @Param("excludeId") Long excludeId);
}
```

- [ ] **Step 2: Create XML result maps**

Use existing mapper style from `EaArchiveDatasourceMapper.xml`. Include BaseEntity columns in each result map: `created_time`, `updated_time`, `creator_id`, `updater_id`, `deleted`.

- [ ] **Step 3: Compile mapper signatures**

Run:

```bash
mvn test -pl easyarchive-starter -DskipTests
```

Expected: mapper interfaces and XML resources compile.

- [ ] **Step 4: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupMapper.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByIdMapper.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupItemByTimeMapper.java easyarchive-starter/src/main/resources/mapper/ArchiveGroupMapper.xml easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByIdMapper.xml easyarchive-starter/src/main/resources/mapper/ArchiveGroupItemByTimeMapper.xml
git commit -m "feat: add archive group mappers"
```

### Task 4: Implement Group Service

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java`

- [ ] **Step 1: Write failing service tests**

Create tests for duplicate code and disabled update:

```java
@Test
void shouldRejectDuplicateGroupCodeOnCreate() {
    ArchiveGroup existing = new ArchiveGroup();
    existing.setId(1L);
    existing.setGroupCode("ORDER_ARCHIVE");
    when(groupMapper.selectByCode("ORDER_ARCHIVE")).thenReturn(existing);

    ArchiveGroup input = new ArchiveGroup();
    input.setGroupCode("ORDER_ARCHIVE");
    input.setGroupName("Order Archive");
    input.setSourceDatasourceId(1L);
    input.setTargetDatasourceId(2L);

    assertThrows(IllegalArgumentException.class, () -> service.create(input));
    verify(groupMapper, never()).insert(any());
}
```

Add this helper in the same test class:

```java
private static ArchiveGroup enabledGroup() {
    ArchiveGroup group = new ArchiveGroup();
    group.setId(10L);
    group.setGroupCode("ORDER_ARCHIVE");
    group.setGroupName("Order Archive");
    group.setSourceDatasourceId(1L);
    group.setTargetDatasourceId(2L);
    group.setEnableStatus(0);
    return group;
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest
```

Expected: fails because service does not exist.

- [ ] **Step 3: Implement group service**

Service interface:

```java
public interface ArchiveGroupService {
    List<ArchiveGroup> findAll(Integer enableStatus);
    List<ArchiveGroup> tree();
    ArchiveGroup findById(Long id);
    ArchiveGroup create(ArchiveGroup group);
    ArchiveGroup update(ArchiveGroup group);
    void updateStatus(Long id, Integer enableStatus);
    void delete(Long id);
}
```

Validation in implementation:

```java
private void validateForSave(ArchiveGroup group, boolean create) {
    if (group.getGroupCode() == null || group.getGroupCode().trim().isEmpty()) {
        throw new IllegalArgumentException("分组编码不能为空");
    }
    if (group.getGroupName() == null || group.getGroupName().trim().isEmpty()) {
        throw new IllegalArgumentException("分组名称不能为空");
    }
    if (group.getSourceDatasourceId() == null || group.getTargetDatasourceId() == null) {
        throw new IllegalArgumentException("源和目标数据源不能为空");
    }
    ArchiveGroup existing = groupMapper.selectByCode(group.getGroupCode());
    if (existing != null && (create || !existing.getId().equals(group.getId()))) {
        throw new IllegalArgumentException("分组编码已存在");
    }
}
```

- [ ] **Step 4: Run group service tests**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupServiceImplTest
```

Expected: tests pass.

- [ ] **Step 5: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImpl.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupServiceImplTest.java
git commit -m "feat: add archive group service"
```

### Task 5: Implement ID and Time Item Services

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupItemByIdService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupItemByTimeService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java`

- [ ] **Step 1: Write failing priority and safety tests**

ID item test:

```java
@Test
void shouldRejectPriorityConflictAcrossBothItemTables() {
    when(groupMapper.selectById(10L)).thenReturn(enabledGroup());
    when(idMapper.countPriority(10L, 5, null)).thenReturn(0);
    when(timeMapper.countPriority(10L, 5, null)).thenReturn(1);

    ArchiveGroupItemById item = validIdItem();
    item.setGroupId(10L);
    item.setPriority(5);

    assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
    verify(idMapper, never()).insert(any());
}
```

Time item test:

```java
@Test
void shouldRejectCleanWithoutWriteWhenEnabled() {
    when(groupMapper.selectById(10L)).thenReturn(enabledGroup());

    ArchiveGroupItemByTime item = validTimeItem();
    item.setEnableStatus(0);
    item.setEnableClean(0);
    item.setEnableWrite(1);

    assertThrows(IllegalArgumentException.class, () -> service.create(10L, item));
    verify(timeMapper, never()).insert(any());
}
```

Add these helpers in the corresponding test classes:

```java
private static ArchiveGroup enabledGroup() {
    ArchiveGroup group = new ArchiveGroup();
    group.setId(10L);
    group.setGroupCode("ORDER_ARCHIVE");
    group.setGroupName("Order Archive");
    group.setSourceDatasourceId(1L);
    group.setTargetDatasourceId(2L);
    group.setEnableStatus(0);
    return group;
}

private static ArchiveGroupItemById validIdItem() {
    ArchiveGroupItemById item = new ArchiveGroupItemById();
    item.setGroupId(10L);
    item.setSourceTable("t_order");
    item.setTargetTable("t_order_archive");
    item.setPriority(10);
    item.setFetchSql("select id from t_order where id >= ? and id < ?");
    item.setDeleteWhere("1 = 1");
    item.setStartId("0");
    item.setEndId("10000");
    item.setStepCount(1000);
    item.setStepRounds(5000);
    item.setEnableClean(0);
    item.setEnableWrite(0);
    item.setEnableStatus(0);
    item.setIdColumn("id");
    return item;
}

private static ArchiveGroupItemByTime validTimeItem() {
    ArchiveGroupItemByTime item = new ArchiveGroupItemByTime();
    item.setGroupId(10L);
    item.setSourceTable("t_order");
    item.setTargetTable("t_order_archive");
    item.setPriority(20);
    item.setFetchSql("select id from t_order where created_time >= ? and created_time < ?");
    item.setDeleteWhere("1 = 1");
    item.setStartTime(new Date(0L));
    item.setKeepDay(30);
    item.setStepMinutes(60);
    item.setStepCount(1000);
    item.setEnableClean(0);
    item.setEnableWrite(0);
    item.setEnableStatus(0);
    item.setIdColumn("id");
    return item;
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest
```

Expected: fails because services do not exist.

- [ ] **Step 3: Implement service interfaces**

ID service:

```java
public interface ArchiveGroupItemByIdService {
    List<ArchiveGroupItemById> findByGroupId(Long groupId, Integer enableStatus);
    ArchiveGroupItemById findById(Long groupId, Long itemId);
    ArchiveGroupItemById create(Long groupId, ArchiveGroupItemById item);
    ArchiveGroupItemById update(Long groupId, Long itemId, ArchiveGroupItemById item);
    void updateStatus(Long groupId, Long itemId, Integer enableStatus);
    void delete(Long groupId, Long itemId);
}
```

Time service:

```java
public interface ArchiveGroupItemByTimeService {
    List<ArchiveGroupItemByTime> findByGroupId(Long groupId, Integer enableStatus);
    ArchiveGroupItemByTime findById(Long groupId, Long itemId);
    ArchiveGroupItemByTime create(Long groupId, ArchiveGroupItemByTime item);
    ArchiveGroupItemByTime update(Long groupId, Long itemId, ArchiveGroupItemByTime item);
    void updateStatus(Long groupId, Long itemId, Integer enableStatus);
    void delete(Long groupId, Long itemId);
}
```

- [ ] **Step 4: Implement shared validation in each service**

Each service implementation must validate group existence, required fields, numeric ranges, cross-table priority conflict, and unsafe clean-without-write.

Use this check in both services:

```java
private void validatePriority(Long groupId, Integer priority, Long excludeId, boolean idItem) {
    if (priority == null) {
        throw new IllegalArgumentException("优先级不能为空");
    }
    int idCount = idMapper.countPriority(groupId, priority, idItem ? excludeId : null);
    int timeCount = timeMapper.countPriority(groupId, priority, idItem ? null : excludeId);
    if (idCount + timeCount > 0) {
        throw new IllegalArgumentException("同一分组内优先级不能重复");
    }
}
```

- [ ] **Step 5: Run item service tests**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupItemByIdServiceImplTest,ArchiveGroupItemByTimeServiceImplTest
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupItemByIdService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupItemByTimeService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImpl.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImpl.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByIdServiceImplTest.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupItemByTimeServiceImplTest.java
git commit -m "feat: add archive group item services"
```

### Task 6: Implement Group-Scoped Loader and Task Trigger

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/rule/PlatformArchiveRuleLoader.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupExecutionService.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/support/ArchiveGroupTaskDispatcher.java`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java`
- Modify: `easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml`
- Modify: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/rule/PlatformArchiveRuleLoaderTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java`

- [ ] **Step 1: Write failing loader test**

```java
@Test
void shouldMergeEnabledItemsSortedByPriority() {
    ArchiveGroupItemById idItem = new ArchiveGroupItemById();
    idItem.setId(1L);
    idItem.setGroupId(10L);
    idItem.setPriority(20);
    idItem.setEnableStatus(0);

    ArchiveGroupItemByTime timeItem = new ArchiveGroupItemByTime();
    timeItem.setId(2L);
    timeItem.setGroupId(10L);
    timeItem.setPriority(10);
    timeItem.setEnableStatus(0);

    when(idMapper.selectByGroupId(10L, 0)).thenReturn(Collections.singletonList(idItem));
    when(timeMapper.selectByGroupId(10L, 0)).thenReturn(Collections.singletonList(timeItem));

    List<ArchiveGroupItem> items = loader.load();

    assertEquals(2, items.size());
    assertTrue(items.get(0) instanceof ArchiveGroupItemByTime);
    assertTrue(items.get(1) instanceof ArchiveGroupItemById);
}
```

- [ ] **Step 2: Write failing trigger service test**

```java
@Test
void shouldRejectTriggerWhenActiveTaskExists() {
    ArchiveGroup group = enabledGroup();
    when(groupMapper.selectById(10L)).thenReturn(group);
    when(taskMapper.countActiveByGroupId(10L)).thenReturn(1);

    assertThrows(IllegalStateException.class, () -> service.trigger(10L));
    verify(taskMapper, never()).insert(any());
    verify(dispatcher, never()).dispatch(any(), any(), any());
}
```

Add this helper in the trigger service test class:

```java
private static ArchiveGroup enabledGroup() {
    ArchiveGroup group = new ArchiveGroup();
    group.setId(10L);
    group.setGroupCode("ORDER_ARCHIVE");
    group.setGroupName("Order Archive");
    group.setSourceDatasourceId(1L);
    group.setTargetDatasourceId(2L);
    group.setEnableStatus(0);
    return group;
}
```

- [ ] **Step 3: Run tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=PlatformArchiveRuleLoaderTest,ArchiveGroupExecutionServiceImplTest
```

Expected: fails because loader and trigger service do not exist.

- [ ] **Step 4: Implement `PlatformArchiveRuleLoader`**

Constructor and load behavior:

```java
public class PlatformArchiveRuleLoader implements ArchiveRuleLoader {
    private final Long groupId;
    private final ArchiveGroupItemByIdMapper idMapper;
    private final ArchiveGroupItemByTimeMapper timeMapper;

    public List<ArchiveGroupItem> load() {
        List<ArchiveGroupItem> result = new ArrayList<>();
        result.addAll(idMapper.selectByGroupId(groupId, 0));
        result.addAll(timeMapper.selectByGroupId(groupId, 0));
        result.sort(Comparator.comparing(ArchiveGroupItem::getPriority));
        return result;
    }
}
```

- [ ] **Step 5: Add active task count mapper method**

Add to `ArchiveGroupExecuteTaskMapper`:

```java
int countActiveByGroupId(@Param("groupId") Long groupId);
```

Add XML:

```xml
<select id="countActiveByGroupId" resultType="int">
    SELECT COUNT(1)
    FROM ea_archive_group_execute_task
    WHERE deleted = 0
      AND group_id = #{groupId}
      AND execute_status IN (0, 1, 4)
</select>
```

- [ ] **Step 6: Implement dispatcher**

```java
public void dispatch(ArchiveRuleLoader loader,
                     ArchiveGroupExecuteTask task,
                     Pair<ArchiveConnection, ArchiveConnection> connections) {
    ArchiveGroupExecutor executor = new ArchiveGroupExecutor(
            loader, archiveConfig, task, connections, publisher, archiveLogRepository);
    executorService.submit(executor);
}
```

Use a bounded `ExecutorService` field:

```java
private final ExecutorService executorService = Executors.newFixedThreadPool(4);
```

- [ ] **Step 7: Implement trigger service**

`ArchiveGroupExecutionService`:

```java
public interface ArchiveGroupExecutionService {
    ArchiveGroupExecuteTask trigger(Long groupId);
}
```

The implementation must:

- Validate group exists and `enableStatus == 0`.
- Reject active task count greater than `0`.
- Reject if both child tables have zero enabled items.
- Load source and target datasources from `EaArchiveDatasourceMapper`.
- Insert `ArchiveGroupExecuteTask` with `WAITING`, `processedRecords = 0L`, `finishedFlag = 0L`.
- Dispatch `ArchiveGroupExecutor` using `PlatformArchiveRuleLoader`.

- [ ] **Step 8: Modify `DbArchiveLogListener` task-start behavior**

Change `handleTaskStart` so it updates the existing trigger-created row instead of always inserting.

Use this logic:

```java
ArchiveGroupExecuteTask existing = repository.queryTaskById(event.getTaskId());
if (existing == null) {
    repository.saveTaskExecution(task);
} else {
    repository.updateTaskExecution(task);
}
```

- [ ] **Step 9: Run trigger tests**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=PlatformArchiveRuleLoaderTest,ArchiveGroupExecutionServiceImplTest
```

Expected: tests pass.

- [ ] **Step 10: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/rule/PlatformArchiveRuleLoader.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/ArchiveGroupExecutionService.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImpl.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/support/ArchiveGroupTaskDispatcher.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/mapper/ArchiveGroupExecuteTaskMapper.java easyarchive-starter/src/main/resources/mapper/ArchiveGroupExecuteTaskMapper.xml easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/listener/DbArchiveLogListener.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/rule/PlatformArchiveRuleLoaderTest.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/service/impl/ArchiveGroupExecutionServiceImplTest.java
git commit -m "feat: trigger archive groups from platform tables"
```

### Task 7: Implement REST Controllers

**Files:**
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemSummary.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java`
- Create: `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemController.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java`
- Create: `easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemControllerContractTest.java`

- [ ] **Step 1: Write failing controller contract test**

Group controller contract:

```java
mockMvc.perform(get("/api/v1/archive/groups"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data").isArray());
```

Item controller contract:

```java
mockMvc.perform(get("/api/v1/archive/groups/10/items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("SUCCESS"))
        .andExpect(jsonPath("$.data").isArray());
```

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupControllerContractTest,ArchiveGroupItemControllerContractTest
```

Expected: fails because controllers do not exist.

- [ ] **Step 3: Implement `ArchiveGroupController`**

Use request mapping:

```java
@RestController
@RequestMapping("/api/v1/archive/groups")
@RequiredArgsConstructor
public class ArchiveGroupController {
    private final ArchiveGroupService groupService;
    private final ArchiveGroupExecutionService executionService;
}
```

Add methods for list, tree, detail, create, update, status, delete, and trigger.

- [ ] **Step 4: Implement `ArchiveGroupItemController`**

Use request mapping:

```java
@RestController
@RequestMapping("/api/v1/archive/groups/{groupId}/items")
@RequiredArgsConstructor
public class ArchiveGroupItemController {
    private final ArchiveGroupItemByIdService idService;
    private final ArchiveGroupItemByTimeService timeService;
}
```

Merged summary DTO:

```java
@Data
public class ArchiveGroupItemSummary {
    private String itemType;
    private Long id;
    private Long groupId;
    private String sourceTable;
    private String targetTable;
    private Integer priority;
    private Integer stepCount;
    private Integer enableWrite;
    private Integer enableClean;
    private Integer enableStatus;
}
```

- [ ] **Step 5: Run controller tests**

Run:

```bash
mvn test -pl easyarchive-starter -Dtest=ArchiveGroupControllerContractTest,ArchiveGroupItemControllerContractTest
```

Expected: tests pass.

- [ ] **Step 6: Commit**

```bash
git add easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/model/dto/ArchiveGroupItemSummary.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupController.java easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemController.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupControllerContractTest.java easyarchive-starter/src/test/java/com/openquartz/easyarchive/starter/controller/ArchiveGroupItemControllerContractTest.java
git commit -m "feat: expose archive group APIs"
```

### Task 8: Implement Frontend API Contracts

**Files:**
- Create: `easyarchive-ui/src/api/archiveGroup.ts`
- Create: `easyarchive-ui/src/api/archiveGroupItem.ts`

- [ ] **Step 1: Create group API module**

`archiveGroup.ts` should export:

```ts
export interface ArchiveGroup {
  id: number;
  parentId?: number;
  groupCode: string;
  groupName: string;
  groupPath?: string;
  groupLevel?: number;
  sourceDatasourceId: number;
  targetDatasourceId: number;
  ownerUserId?: number;
  enableStatus: number;
  triggerMode?: string;
  remark?: string;
  createdTime?: string;
  updatedTime?: string;
}

export type ArchiveGroupPayload = Omit<ArchiveGroup, "id" | "createdTime" | "updatedTime">;
```

Functions:

```ts
getArchiveGroupsApi()
getArchiveGroupTreeApi()
getArchiveGroupApi(id: number)
createArchiveGroupApi(payload: ArchiveGroupPayload)
updateArchiveGroupApi(id: number, payload: ArchiveGroupPayload)
updateArchiveGroupStatusApi(id: number, enableStatus: number)
deleteArchiveGroupApi(id: number)
triggerArchiveGroupApi(id: number)
```

- [ ] **Step 2: Create item API module**

`archiveGroupItem.ts` should export `ArchiveGroupItemSummary`, `ArchiveGroupItemById`, `ArchiveGroupItemByTime`, and CRUD functions for ID and TIME item endpoints.

- [ ] **Step 3: Run TypeScript compile**

Run:

```bash
cd easyarchive-ui && npm run build
```

Expected: build passes or fails only because route/view files are not created yet. If route/view files are not referenced, build should pass.

- [ ] **Step 4: Commit**

```bash
git add easyarchive-ui/src/api/archiveGroup.ts easyarchive-ui/src/api/archiveGroupItem.ts
git commit -m "feat: add archive group frontend APIs"
```

### Task 9: Implement Frontend Group Page and Forms

**Files:**
- Create: `easyarchive-ui/src/views/ArchiveGroupView.vue`
- Create: `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- Create: `easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue`
- Create: `easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue`
- Modify: `easyarchive-ui/src/router/index.ts`
- Modify: `easyarchive-ui/src/layouts/AppLayout.vue`
- Modify: `easyarchive-ui/src/i18n/messages.ts`
- Modify: `easyarchive-ui/src/utils/dictionaries.ts`
- Modify: `easyarchive-ui/scripts/smoke-check.mjs`

- [ ] **Step 1: Add route and nav**

Router child route:

```ts
{
  path: "archive/groups",
  name: "archive-groups",
  component: ArchiveGroupView
}
```

Sidebar nav:

```vue
<RouterLink class="nav__item" :to="{ name: 'archive-groups' }">{{ t("layout.nav.archiveGroups") }}</RouterLink>
```

- [ ] **Step 2: Implement group page state**

`ArchiveGroupView.vue` should load groups and datasources, track selected group, load merged items for the selected group, and expose actions for create/edit/status/delete/trigger.

Use existing patterns from `DatasourceView.vue`: `loading`, `errorMessage`, `successMessage`, `actionErrorMessage`, `busyRows`, and dialog state refs.

- [ ] **Step 3: Implement forms**

`ArchiveGroupFormDialog.vue` validates:

- `groupCode` required.
- `groupName` required.
- `sourceDatasourceId` required.
- `targetDatasourceId` required.

`ArchiveGroupItemByIdFormDialog.vue` validates:

- `sourceTable`, `targetTable`, `idColumn`, `fetchSql`, `startId`, `endId` required.
- `priority`, `stepCount`, `stepRounds` positive.

`ArchiveGroupItemByTimeFormDialog.vue` validates:

- `sourceTable`, `targetTable`, `idColumn`, `fetchSql`, `startTime` required.
- `priority`, `stepCount`, `stepMinutes` positive.
- `keepDay >= 0`.

- [ ] **Step 4: Update i18n messages**

Add `layout.nav.archiveGroups` and an `archiveGroup` section in both `en-US` and `zh-CN`.

- [ ] **Step 5: Update smoke check**

Add required files and route fragments:

```js
"src/views/ArchiveGroupView.vue",
"src/api/archiveGroup.ts",
"src/api/archiveGroupItem.ts"
```

Route fragment:

```js
'path: "archive/groups"'
```

- [ ] **Step 6: Run frontend verification**

Run:

```bash
cd easyarchive-ui && npm run smoke
```

Expected: TypeScript build, Vite build, and smoke script pass.

- [ ] **Step 7: Commit**

```bash
git add easyarchive-ui/src/views/ArchiveGroupView.vue easyarchive-ui/src/components/ArchiveGroupFormDialog.vue easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue easyarchive-ui/src/router/index.ts easyarchive-ui/src/layouts/AppLayout.vue easyarchive-ui/src/i18n/messages.ts easyarchive-ui/src/utils/dictionaries.ts easyarchive-ui/scripts/smoke-check.mjs
git commit -m "feat: add archive group management UI"
```

### Task 10: Full Verification

**Files:**
- No new files.
- Verify all modified backend and frontend files.

- [ ] **Step 1: Run core tests**

```bash
mvn test -pl easyarchive-core
```

Expected: all core tests pass.

- [ ] **Step 2: Run starter tests**

```bash
mvn test -pl easyarchive-starter
```

Expected: all starter tests pass.

- [ ] **Step 3: Run frontend smoke**

```bash
cd easyarchive-ui && npm run smoke
```

Expected: build and smoke pass.

- [ ] **Step 4: Inspect final diff**

```bash
git status --short
git diff --stat
```

Expected: only intentional uncommitted files remain. If prior workspace changes are still present, do not revert them.

- [ ] **Step 5: Commit verification-only fixes if needed**

If verification required code fixes, commit them:

```bash
git add <fixed-files>
git commit -m "fix: stabilize archive group management"
```

If no fixes were required, do not create an empty commit.

---

## Self-Review

Spec coverage:

- `ea_archive_rule` is removed from the new platform flow through schema edits and V3 drop migration.
- `ArchiveGroup` management is covered by mapper, service, controller, and UI tasks.
- `ArchiveGroupItemById` and `ArchiveGroupItemByTime` have separate tables, mappers, services, controllers, and forms.
- Group-scoped execution is covered by `PlatformArchiveRuleLoader`, trigger service, dispatcher, and task listener update behavior.
- Testing covers service validation, loader ordering, trigger guards, controller contracts, Maven tests, and frontend smoke.

Placeholder scan:

- The plan does not use incomplete requirement markers.
- Each task includes concrete file paths, commands, and expected outcomes.

Type consistency:

- Group ids use `Long` in Java and `number` in TypeScript.
- Group datasource fields consistently use `sourceDatasourceId` and `targetDatasourceId`.
- Child item priority uses `priority` consistently across entity, table, service, and UI.
