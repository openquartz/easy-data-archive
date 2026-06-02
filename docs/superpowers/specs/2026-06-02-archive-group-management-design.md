# ArchiveGroup Management Full-Chain Design

## 1. Goal

Implement the full ArchiveGroup management chain from the web UI to backend services and the archive execution engine.

This feature lets an archive administrator:

- Create, edit, enable, disable, and delete archive groups.
- Manage archive group items inside a group using two concrete item types:
  - `ArchiveGroupItemById`
  - `ArchiveGroupItemByTime`
- Trigger a group manually and view execution progress in the existing task center.

The platform must not use a generic `ea_archive_rule` table for this feature. Rule persistence is split by archive strategy and maps directly to the existing core execution entities.

## 2. Current State

The core execution model already exists in `easyarchive-core`:

- `com.openquartz.easyarchive.core.rule.entity.ArchiveGroup`
- `com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItem`
- `com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemById`
- `com.openquartz.easyarchive.core.rule.entity.ArchiveGroupItemByTime`
- `com.openquartz.easyarchive.core.ArchiveGroupExecutor`
- `com.openquartz.easyarchive.core.executor.ArchiveExecutor`

The platform database already has `ea_archive_group`, but the active starter module does not yet provide group or item management controllers, services, mappers, or UI pages.

The current `DbArchiveRuleLoader` loads from a legacy single config table configured by `sync.config.table`. The new platform flow must instead load only the selected group and its enabled child items from the platform tables.

## 3. Data Model

### 3.1 Archive Group

Use `ea_archive_group` as the parent table.

Required fields:

| Column | Meaning |
| --- | --- |
| `id` | Primary key |
| `parent_id` | Optional parent group |
| `group_code` | Unique group code |
| `group_name` | Display name |
| `group_path` | Materialized tree path |
| `group_level` | Tree level |
| `source_datasource_id` | Source datasource id |
| `target_datasource_id` | Target datasource id |
| `owner_user_id` | Owner user id |
| `enable_status` | `0` enabled, `1` disabled |
| `trigger_mode` | `MANUAL` for this phase |
| `remark` | Description |

`ArchiveGroup` in `easyarchive-core` should be aligned with this table. Use `Long id`, `Long sourceDatasourceId`, and `Long targetDatasourceId` instead of the current mixed `Integer` and `sourceConnectionId` naming.

### 3.2 ID Item Table

Create `ea_archive_group_item_by_id`, mapped directly to `ArchiveGroupItemById`.

Required fields:

| Column | Entity Field | Meaning |
| --- | --- | --- |
| `id` | `id` | Primary key |
| `group_id` | `groupId` | Parent group |
| `source_table` | `sourceTable` | Source table expression |
| `target_table` | `targetTable` | Target table expression |
| `priority` | `priority` | Group-level execution priority |
| `fetch_sql` | `fetchSql` | Fetch SQL template |
| `delete_where` | `deleteWhere` | Source delete guard |
| `start_id` | `startId` | Start id expression |
| `end_id` | `endId` | End id expression |
| `step_count` | `stepCount` | Batch size |
| `step_rounds` | `stepRounds` | ID rolling window size |
| `pause_ms` | `pauseMs` | Optional rule pause |
| `enable_clean` | `enableClean` | `0` clean source, `1` do not clean |
| `enable_write` | `enableWrite` | `0` write target, `1` do not write |
| `enable_status` | `enableStatus` | `0` enabled, `1` disabled |
| `id_column` | `idColumn` | Primary id column |

Indexes:

- `idx_group_status(group_id, enable_status)`
- `idx_group_priority_id(group_id, priority)`

Priority is unique across the whole group, including both ID and TIME items. Because this uniqueness spans two child tables, the service layer must check both tables before insert, update, and enable operations. The database indexes are supporting indexes, not the full cross-table constraint.

### 3.3 Time Item Table

Create `ea_archive_group_item_by_time`, mapped directly to `ArchiveGroupItemByTime`.

Required fields:

| Column | Entity Field | Meaning |
| --- | --- | --- |
| `id` | `id` | Primary key |
| `group_id` | `groupId` | Parent group |
| `source_table` | `sourceTable` | Source table expression |
| `target_table` | `targetTable` | Target table expression |
| `priority` | `priority` | Group-level execution priority |
| `fetch_sql` | `fetchSql` | Fetch SQL template |
| `delete_where` | `deleteWhere` | Source delete guard |
| `start_time` | `startTime` | Archive window start |
| `keep_day` | `keepDay` | Retention days |
| `step_minutes` | `stepMinutes` | Time rolling window in minutes |
| `step_count` | `stepCount` | Batch size |
| `pause_ms` | `pauseMs` | Optional rule pause |
| `enable_clean` | `enableClean` | `0` clean source, `1` do not clean |
| `enable_write` | `enableWrite` | `0` write target, `1` do not write |
| `enable_status` | `enableStatus` | `0` enabled, `1` disabled |
| `id_column` | `idColumn` | Primary id column |

Indexes:

- `idx_group_status(group_id, enable_status)`
- `idx_group_priority_time(group_id, priority)`

Because group priority should be unique across both item tables, service-level validation must check both tables before create or update.

## 4. Backend Architecture

### 4.1 New Backend Components

Add group management:

- `ArchiveGroupController`
- `ArchiveGroupService`
- `ArchiveGroupServiceImpl`
- `ArchiveGroupMapper`
- `ArchiveGroupMapper.xml`

Add item management:

- `ArchiveGroupItemController`
- `ArchiveGroupItemByIdService`
- `ArchiveGroupItemByIdServiceImpl`
- `ArchiveGroupItemByTimeService`
- `ArchiveGroupItemByTimeServiceImpl`
- `ArchiveGroupItemByIdMapper`
- `ArchiveGroupItemByIdMapper.xml`
- `ArchiveGroupItemByTimeMapper`
- `ArchiveGroupItemByTimeMapper.xml`

Add execution trigger support:

- `ArchiveGroupExecutionService`
- `ArchiveGroupExecutionServiceImpl`
- `ArchiveGroupTaskDispatcher`
- `PlatformArchiveRuleLoader`

`PlatformArchiveRuleLoader` implements `ArchiveRuleLoader` and accepts a `groupId`. It queries enabled ID and TIME child rows, merges them into `List<ArchiveGroupItem>`, and sorts by `priority`.

### 4.2 API Design

Group APIs:

- `GET /api/v1/archive/groups`
- `GET /api/v1/archive/groups/tree`
- `GET /api/v1/archive/groups/{id}`
- `POST /api/v1/archive/groups`
- `PUT /api/v1/archive/groups/{id}`
- `PATCH /api/v1/archive/groups/{id}/status?enableStatus=0|1`
- `DELETE /api/v1/archive/groups/{id}`
- `POST /api/v1/archive/groups/{id}/trigger`

Unified item read API:

- `GET /api/v1/archive/groups/{groupId}/items`

This returns a merged list with a synthetic `itemType` field:

```json
{
  "itemType": "ID",
  "id": 1,
  "groupId": 10,
  "sourceTable": "t_order",
  "targetTable": "t_order_archive",
  "priority": 10
}
```

ID item APIs:

- `GET /api/v1/archive/groups/{groupId}/items/id`
- `POST /api/v1/archive/groups/{groupId}/items/id`
- `GET /api/v1/archive/groups/{groupId}/items/id/{itemId}`
- `PUT /api/v1/archive/groups/{groupId}/items/id/{itemId}`
- `PATCH /api/v1/archive/groups/{groupId}/items/id/{itemId}/status?enableStatus=0|1`
- `DELETE /api/v1/archive/groups/{groupId}/items/id/{itemId}`

Time item APIs:

- `GET /api/v1/archive/groups/{groupId}/items/time`
- `POST /api/v1/archive/groups/{groupId}/items/time`
- `GET /api/v1/archive/groups/{groupId}/items/time/{itemId}`
- `PUT /api/v1/archive/groups/{groupId}/items/time/{itemId}`
- `PATCH /api/v1/archive/groups/{groupId}/items/time/{itemId}/status?enableStatus=0|1`
- `DELETE /api/v1/archive/groups/{groupId}/items/time/{itemId}`

### 4.3 Validation Rules

Group validation:

- `groupCode` is required and unique.
- `groupName` is required.
- `sourceDatasourceId` and `targetDatasourceId` are required.
- Referenced datasources must exist and must not be disabled before triggering.
- Disabled groups cannot be triggered.
- A group cannot be deleted if it has enabled child items.
- A group cannot be deleted if it has a waiting, running, or cancelling task.

Item validation:

- `groupId` must exist.
- `sourceTable`, `targetTable`, `fetchSql`, and `idColumn` are required.
- `priority` must be unique across both item tables for the same group.
- `stepCount` must be greater than `0`.
- `pauseMs` must be `null` or greater than or equal to `0`.
- `enableClean = 0` and `enableWrite = 1` is not allowed when enabling an item, because that would delete source data without writing target data.

ID item validation:

- `startId` and `endId` are required.
- `stepRounds` must be greater than `0`.

Time item validation:

- `startTime` is required.
- `keepDay` must be greater than or equal to `0`.
- `stepMinutes` must be greater than `0`.

## 5. Execution Flow

Manual trigger flow:

1. UI calls `POST /api/v1/archive/groups/{id}/trigger`.
2. `ArchiveGroupExecutionService` loads and validates the group.
3. Service checks no active task exists for the group.
4. Service loads source and target datasource records.
5. Service creates `ArchiveGroupExecuteTask` with `WAITING` status and generated id.
6. Dispatcher submits an async `ArchiveGroupExecutor`.
7. The executor uses `PlatformArchiveRuleLoader(groupId)` instead of the legacy global config loader.
8. Loader queries both child item tables where `enable_status = 0`.
9. Loader merges `ArchiveGroupItemById` and `ArchiveGroupItemByTime` rows.
10. Executor sorts by `ArchiveGroupItem.getPriority()` and runs existing `ArchiveExecutor`.
11. Existing event publisher and `DbArchiveLogListener` update task status, progress, and logs.
12. UI redirects to task detail or task list and uses existing polling.

Task creation and event handling must avoid duplicate inserts. The trigger service owns the initial insert. `TaskStartEvent` handling should update the existing row to `RUNNING` instead of inserting another row when the task id already exists.

## 6. Frontend Design

### 6.1 New Files

Add API modules:

- `easyarchive-ui/src/api/archiveGroup.ts`
- `easyarchive-ui/src/api/archiveGroupItem.ts`

Add views and components:

- `easyarchive-ui/src/views/ArchiveGroupView.vue`
- `easyarchive-ui/src/components/ArchiveGroupFormDialog.vue`
- `easyarchive-ui/src/components/ArchiveGroupItemByIdFormDialog.vue`
- `easyarchive-ui/src/components/ArchiveGroupItemByTimeFormDialog.vue`

Add route and navigation:

- Route: `/archive/groups`
- Route name: `archive-groups`
- Sidebar label: `归档分组` / `Archive Groups`

### 6.2 Page Behavior

The ArchiveGroup page has three areas:

- Group list or tree.
- Selected group summary.
- Selected group item table.

Group actions:

- Create group.
- Edit group.
- Enable or disable group.
- Delete group.
- Trigger archive.

Item actions:

- Add ID item.
- Add Time item.
- Edit item.
- Enable or disable item.
- Delete item.

The merged item table shows:

- Type: `ID` or `TIME`
- Priority
- Source table
- Target table
- Batch size
- Write enabled
- Clean enabled
- Status
- Actions

The ID and Time dialogs are separate because their required fields differ. This keeps form validation straightforward and mirrors the backend tables.

## 7. Error Handling

Backend service methods throw `IllegalArgumentException` for invalid input and `IllegalStateException` for invalid state transitions. Controllers can return `ApiResponse.error(...)` for these cases, matching existing task cancellation behavior.

Important error codes:

- `GROUP_NOT_FOUND`
- `GROUP_DISABLED`
- `DATASOURCE_DISABLED`
- `ITEM_NOT_FOUND`
- `PRIORITY_CONFLICT`
- `ACTIVE_TASK_EXISTS`
- `UNSAFE_CLEAN_WITHOUT_WRITE`
- `INVALID_ITEM_CONFIG`

Execution failures continue to be recorded through `TaskEndEvent` and task logs.

## 8. Testing Strategy

Backend tests:

- `ArchiveGroupServiceImplTest`
- `ArchiveGroupItemByIdServiceImplTest`
- `ArchiveGroupItemByTimeServiceImplTest`
- `PlatformArchiveRuleLoaderTest`
- `ArchiveGroupExecutionServiceImplTest`
- Controller contract tests for group and item endpoints.

Required cases:

- Create group succeeds.
- Duplicate `groupCode` fails.
- Disabled group cannot be triggered.
- Group without enabled items cannot be triggered.
- Group with active task cannot be triggered.
- ID item create/update validates `startId`, `endId`, and `stepRounds`.
- Time item create/update validates `startTime`, `keepDay`, and `stepMinutes`.
- Priority conflict across ID and Time tables fails.
- Unsafe clean-without-write configuration cannot be enabled.
- Loader returns both item types sorted by priority.

Frontend checks:

- `npm run build`
- Smoke check for route loading.
- Smoke check for creating/editing group payload shape.
- Smoke check for ID and Time item payload shapes.

Maven checks:

- `mvn test -pl easyarchive-core`
- `mvn test -pl easyarchive-starter`

## 9. Implementation Boundaries

This design intentionally does not implement:

- Scheduled trigger mode.
- `ea_archive_rule` or generic rule-condition tables.
- Visual SQL builder.
- Role and permission enforcement beyond the existing authenticated API pattern.
- Cross-database schema introspection for table/column suggestions.

These can be added later without changing the split item-table model.

## 10. Open Decisions Resolved

- Source of truth: `ea_archive_group` plus child item tables.
- Generic `ea_archive_rule`: removed from the new design.
- Execution entity mapping: direct table-to-entity mapping for `ArchiveGroupItemById` and `ArchiveGroupItemByTime`.
- UI layout: one group management page with separate ID and Time item dialogs.
