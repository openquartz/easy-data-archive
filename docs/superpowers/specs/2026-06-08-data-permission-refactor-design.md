# EasyArchive Data Permission Refactor Design

## Background

`easy-archive-master` currently has a partial management-side authorization model, but the effective runtime rules are still much narrower than the current product needs.

Existing implementation characteristics:

- `sys_user.role_code` is the main role signal used at runtime
- `ea_user_datasource_permission` stores only a single datasource permission type, effectively `READ`
- `DataPermissionServiceImpl` mainly distinguishes only `platform_admin` versus non-admin users
- datasource, archive-group, and task access is mostly derived from one datasource-side check, not a full resource policy
- schema tables such as `sys_role`, `sys_permission`, `sys_user_role`, and `sys_role_permission` exist, but are not the real authorization source of truth

This leaves several capability gaps for the new management model:

- it cannot express separate datasource management permission versus datasource usage permission
- it cannot express that an archive administrator may manage only some datasources
- it cannot express that an archive administrator may grant access only within their managed datasource scope
- it cannot safely support normal users creating and editing archive tasks using only authorized datasources
- current group and task access checks are too weak because they do not consistently validate both source and target datasources

The new requirement is a clean rebuild of the data permission model. Compatibility with the old model is not required at the design level, but an implementation migration path still needs to preserve existing usable data where practical.

## Confirmed Business Rules

The design below is based on the following confirmed rules:

1. the system supports exactly three runtime roles:
   - system administrator
   - platform archive administrator
   - normal user
2. a platform archive administrator may create only normal users
3. a platform archive administrator may assign datasource permissions to normal users only within the datasources that the administrator already manages
4. normal users have all business-operation permissions except user management and datasource management
5. when normal users create or edit archive tasks or archive groups, they may select only datasources they are authorized to use
6. system administrators have unrestricted platform access

## Goal

Introduce a new authorization model that cleanly separates:

- platform-level capabilities controlled by role
- datasource-level access controlled by explicit resource authorization
- archive-group and task access derived from datasource authorization

The resulting design must support:

- datasource creation being exclusive to system administrators
- datasource editing and disabling by system administrators or explicitly authorized archive administrators
- normal-user task and archive-group operations using only authorized datasources
- explicit, auditable datasource authorization assignment and revocation
- consistent backend enforcement independent of frontend UI hiding

## Non-Goals

Out of scope for this refactor:

- a generic multi-role IAM platform
- enabling runtime authorization from the existing `sys_role_permission` graph
- row-level authorization models beyond datasource scope
- compatibility behavior that preserves old role names or old UI semantics forever
- unrelated broad service refactors outside the authorization domain

## Recommended Approach

### Chosen Model

Use a two-layer model:

1. role-based platform capability control
2. explicit datasource authorization for resource scope

Archive-group and task access will be derived from the datasource authorizations referenced by those business resources.

### Rejected Simpler Option

Do not use only `role_code` plus `owner_user_id` inference.

Reasons:

- ownership is one-to-one oriented and does not fit many-to-many authorization
- ownership cannot distinguish manage permission from use permission
- ownership does not model delegated authorization safely
- later logic would drift into inconsistent mixed checks across services

### Deferred Heavier Option

Do not fully activate `sys_role`, `sys_permission`, `sys_user_role`, and `sys_role_permission` as the runtime authorization engine in this refactor.

Reasons:

- current requirements are fixed and small
- implementation cost would be disproportionate
- a dedicated capability service can preserve an upgrade path without forcing a generic IAM system now

## Target Authorization Model

### Runtime Roles

Use exactly these normalized role codes:

- `SYSTEM_ADMIN`
- `ARCHIVE_ADMIN`
- `NORMAL_USER`

Role meaning:

- `SYSTEM_ADMIN`: unrestricted platform authority
- `ARCHIVE_ADMIN`: scoped datasource management authority plus broad archive-business authority
- `NORMAL_USER`: broad archive-business authority without user-management or datasource-management authority

### Platform Capabilities

Platform capabilities define whether a role may attempt a category of action before resource scope is checked.

Recommended capability set:

- `USER_CREATE_ARCHIVE_ADMIN`
- `USER_CREATE_NORMAL_USER`
- `USER_EDIT_ROLE`
- `USER_EDIT_BASIC_INFO`
- `USER_UPDATE_STATUS`
- `DATASOURCE_CREATE`
- `DATASOURCE_EDIT_AUTHORIZED`
- `DATASOURCE_DISABLE_AUTHORIZED`
- `DATASOURCE_TEST_AUTHORIZED`
- `DATASOURCE_ASSIGN_MANAGE`
- `DATASOURCE_ASSIGN_USE`
- `ARCHIVE_GROUP_CREATE`
- `ARCHIVE_GROUP_EDIT`
- `ARCHIVE_GROUP_VIEW`
- `ARCHIVE_GROUP_TRIGGER`
- `ARCHIVE_GROUP_CANCEL`
- `TASK_VIEW`
- `TASK_LOG_VIEW`
- `DASHBOARD_VIEW`
- `OPERATION_LOG_VIEW`
- `NOTIFICATION_VIEW`

Recommended static mapping:

#### `SYSTEM_ADMIN`

Has all capabilities.

#### `ARCHIVE_ADMIN`

Has:

- `USER_CREATE_NORMAL_USER`
- `USER_EDIT_BASIC_INFO`
- `USER_UPDATE_STATUS`
- `DATASOURCE_EDIT_AUTHORIZED`
- `DATASOURCE_DISABLE_AUTHORIZED`
- `DATASOURCE_TEST_AUTHORIZED`
- `DATASOURCE_ASSIGN_USE`
- archive-business capabilities such as group, task, dashboard, notification, and log viewing

Does not have:

- `USER_CREATE_ARCHIVE_ADMIN`
- `USER_EDIT_ROLE`
- `DATASOURCE_CREATE`
- `DATASOURCE_ASSIGN_MANAGE`

#### `NORMAL_USER`

Has archive-business capabilities such as:

- `ARCHIVE_GROUP_CREATE`
- `ARCHIVE_GROUP_EDIT`
- `ARCHIVE_GROUP_VIEW`
- `ARCHIVE_GROUP_TRIGGER`
- `ARCHIVE_GROUP_CANCEL`
- `TASK_VIEW`
- `TASK_LOG_VIEW`
- `DASHBOARD_VIEW`
- `NOTIFICATION_VIEW`

Does not have:

- any user-management capability
- any datasource-management capability
- any datasource-assignment capability

### Datasource Authorization Levels

Datasource authorization is the real resource-scope source of truth.

Use exactly two datasource permission levels:

- `MANAGE`
- `USE`

Semantics:

- `MANAGE`
  - intended for `ARCHIVE_ADMIN`
  - allows view, edit, disable, test, and downstream grant of `USE` to normal users
- `USE`
  - intended for `NORMAL_USER`
  - allows datasource selection in archive-group and task business flows
  - does not allow datasource editing, disabling, testing, or further delegation

Coverage rule:

- `MANAGE` covers `USE`
- `USE` does not cover `MANAGE`

This rule is required so archive administrators can operate archive business with datasources they manage without requiring duplicate explicit `USE` grants.

### Owner Fields

Keep `owner_user_id` in datasource and archive-group records only as:

- business owner display field
- notification default recipient anchor
- reporting and responsibility marker

Do not use `owner_user_id` as authorization truth.

Authorization truth must be:

- role capabilities for platform authority
- datasource authorization records for resource scope

## Data Model Design

### User Role Storage

Keep single-role storage on `sys_user.role_code`.

Do not use `sys_user_role` in runtime logic for this refactor.

Normalize all persisted role values to:

- `SYSTEM_ADMIN`
- `ARCHIVE_ADMIN`
- `NORMAL_USER`

### Datasource Authorization Table

Replace `ea_user_datasource_permission` with a new table named `ea_archive_connection_permission`.

Recommended schema:

```sql
CREATE TABLE ea_archive_connection_permission (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    datasource_id BIGINT NOT NULL COMMENT '归档连接ID',
    permission_level VARCHAR(32) NOT NULL COMMENT 'MANAGE/USE',
    grant_source VARCHAR(32) NOT NULL COMMENT 'SYSTEM_ASSIGN/ARCHIVE_ADMIN_ASSIGN',
    granted_by_user_id BIGINT NOT NULL COMMENT '授权人ID',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_ds_level (user_id, datasource_id, permission_level, deleted),
    KEY idx_user_level (user_id, permission_level),
    KEY idx_ds_level (datasource_id, permission_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='归档连接授权表';
```

Field meaning:

- `permission_level`: resource-scope authority level
- `grant_source`: assignment path used for audit and governance
- `granted_by_user_id`: operator identity for audit and traceability

Recommended `grant_source` values:

- `SYSTEM_ASSIGN`
- `ARCHIVE_ADMIN_ASSIGN`

### No Separate Group Or Task Permission Tables

Do not introduce:

- `archive_group_permission`
- `archive_task_permission`

Reason:

- archive groups and tasks are defined by referenced datasources
- adding more permission tables would duplicate the same scope model
- datasource scope is sufficient and simpler

Instead, derive group and task authorization from datasource authorization.

## Authorization Decision Rules

### Global Bypass

`SYSTEM_ADMIN` bypasses all datasource-scope checks and is treated as implicitly holding:

- `MANAGE`
- `USE`

for every datasource.

### Datasource Access Rules

#### `SYSTEM_ADMIN`

- may create any datasource
- may edit, disable, test, and view any datasource
- may assign `MANAGE` to archive administrators
- may assign `USE` to normal users

#### `ARCHIVE_ADMIN`

- may not create datasources
- may edit, disable, test, and view only datasources for which they hold `MANAGE`
- may assign only `USE`
- may assign only to `NORMAL_USER`
- may assign only datasources for which they hold `MANAGE`
- may not assign `MANAGE`

#### `NORMAL_USER`

- may not create, edit, disable, test, or assign datasources
- may use only datasources for which they hold `USE`, or which are covered by `MANAGE` if such a record ever exists in future extension

### User Management Rules

#### `SYSTEM_ADMIN`

May:

- create `ARCHIVE_ADMIN`
- create `NORMAL_USER`
- edit any user
- change any user role except protected bootstrap restrictions if implementation adds them
- update status for any user except protected bootstrap restrictions if implementation adds them

#### `ARCHIVE_ADMIN`

May:

- create only `NORMAL_USER`
- edit only `NORMAL_USER`
- update status only for `NORMAL_USER`
- edit only basic information and password for `NORMAL_USER`

May not:

- create `SYSTEM_ADMIN`
- create `ARCHIVE_ADMIN`
- edit any user role
- manage system administrators or peer archive administrators

#### `NORMAL_USER`

May not access user-management functions.

### Datasource Delegation Rules

These rules must be enforced in backend services, not only in frontend behavior.

1. system administrators assigning datasource authority to archive administrators may assign only `MANAGE`
2. system administrators assigning datasource authority to normal users may assign only `USE`
3. archive administrators assigning datasource authority may assign only `USE`
4. archive administrators may assign datasource authority only to normal users
5. archive administrators may assign datasource authority only for datasources they currently hold with `MANAGE`
6. normal users may assign no datasource authority

### Archive Group Access Rules

An archive group is authorized by its full datasource set:

- `source_datasource_id`
- `target_datasource_id`

Rules:

- `SYSTEM_ADMIN` may view, create, edit, trigger, and cancel any archive group
- `ARCHIVE_ADMIN` may do those actions only when both source and target datasources are within `MANAGE` scope
- `NORMAL_USER` may do those actions only when both source and target datasources are within `USE` scope, with `MANAGE` covering `USE`

This applies to:

- list
- detail
- create
- edit
- trigger
- cancel

### Task Access Rules

Task authorization is derived through the owning archive group.

Rules:

- a user may view a task only if the task's group is readable in that user's datasource scope
- a user may view task logs only if the task is readable
- a user may cancel a task only if the owning group is cancellable for that user

Critical correction:

- task and group access checks must validate both source and target datasource scope
- source-only validation is insufficient and must be removed

## Backend Service Design

### `CurrentUserService`

Purpose:

- resolve the authenticated user from Spring Security context
- return a normalized runtime user object

Recommended output:

- `userId`
- `username`
- `roleCode`

### `RoleCapabilityService`

Purpose:

- answer whether the current role may attempt a platform-level action

Recommended methods:

- `assertCapability(PlatformCapabilityEnum capability, CurrentUser currentUser)`
- `boolean hasCapability(PlatformCapabilityEnum capability, CurrentUser currentUser)`

Implementation style:

- static role-to-capability mapping
- no database dependency required

### `DatasourceAuthorizationService`

Purpose:

- query datasource authorization records
- apply `MANAGE` covers `USE` logic
- enforce datasource-scope checks and delegation rules

Recommended methods:

- `Set<Long> listDatasourceIdsByLevel(Long userId, DatasourcePermissionLevelEnum level)`
- `boolean hasPermission(Long userId, Long datasourceId, DatasourcePermissionLevelEnum requiredLevel)`
- `void assertPermission(Long userId, Long datasourceId, DatasourcePermissionLevelEnum requiredLevel)`
- `void assertGrantable(CurrentUser currentUser, SysUser targetUser, Long datasourceId, DatasourcePermissionLevelEnum level)`
- `List<ArchiveConnectionPermission> listAuthorizations(Long userId)`
- `void replaceAuthorizations(Long targetUserId, List<AuthorizationCommand> commands, CurrentUser operator)`

Decision policy:

- if required level is `USE`, both explicit `USE` and explicit `MANAGE` satisfy the check
- if required level is `MANAGE`, only explicit `MANAGE` satisfies the check
- `SYSTEM_ADMIN` always passes

### `ArchiveResourceAccessService`

Purpose:

- map archive groups and tasks back to datasource authorization
- centralize group and task authorization rules

Recommended methods:

- `void assertGroupReadable(Long groupId)`
- `void assertGroupEditable(Long groupId)`
- `void assertGroupTriggerable(Long groupId)`
- `void assertGroupCancellable(Long groupId)`
- `void assertTaskReadable(Long taskId)`
- `void assertTaskLogReadable(Long taskId)`

Required behavior:

- load the group or task
- resolve source and target datasource IDs
- compute required datasource permission level from current role
- validate both datasource references

### Legacy `DataPermissionService`

Do not keep `DataPermissionServiceImpl` as the long-term authority decision engine.

Migration approach:

1. introduce the new services above
2. shift callers away from legacy methods
3. remove or reduce `DataPermissionService` to a thin compatibility adapter if still temporarily needed

## API Design

### Auth API

`POST /api/v1/auth/me` should return role and capability information, not only `isAdmin`.

Recommended response shape:

```json
{
  "id": 3,
  "username": "zhangsan",
  "realName": "张三",
  "status": 0,
  "roleCode": "ARCHIVE_ADMIN",
  "capabilities": [
    "USER_CREATE_NORMAL_USER",
    "DATASOURCE_EDIT_AUTHORIZED",
    "DATASOURCE_DISABLE_AUTHORIZED",
    "DATASOURCE_TEST_AUTHORIZED",
    "DATASOURCE_ASSIGN_USE",
    "ARCHIVE_GROUP_CREATE",
    "ARCHIVE_GROUP_EDIT",
    "ARCHIVE_GROUP_VIEW",
    "ARCHIVE_GROUP_TRIGGER",
    "ARCHIVE_GROUP_CANCEL",
    "TASK_VIEW",
    "TASK_LOG_VIEW",
    "DASHBOARD_VIEW",
    "NOTIFICATION_VIEW"
  ]
}
```

### User APIs

#### Create User

`POST /api/v1/users`

Rules:

- `SYSTEM_ADMIN` may create `ARCHIVE_ADMIN` or `NORMAL_USER`
- `ARCHIVE_ADMIN` may create only `NORMAL_USER`
- backend must reject illegal target role combinations explicitly

#### Update User

`PUT /api/v1/users/{id}`

Rules:

- `SYSTEM_ADMIN` may edit user role and base data
- `ARCHIVE_ADMIN` may edit only `NORMAL_USER` basic info and password
- `ARCHIVE_ADMIN` may not modify role

#### Update User Status

`PUT /api/v1/users/{id}/status`

Rules:

- `SYSTEM_ADMIN` may update status broadly
- `ARCHIVE_ADMIN` may update only `NORMAL_USER`

### Datasource Authorization APIs

Replace the current datasource-permission contract with datasource-authorization semantics.

Recommended endpoints:

- `GET /api/v1/users/{userId}/datasource-authorizations`
- `PUT /api/v1/users/{userId}/datasource-authorizations`

Recommended response item:

```json
{
  "datasourceId": 12,
  "datasourceCode": "order_src",
  "datasourceName": "订单源库",
  "permissionLevel": "USE",
  "grantedByUserId": 2,
  "grantSource": "ARCHIVE_ADMIN_ASSIGN"
}
```

Recommended replace request:

```json
{
  "authorizations": [
    {
      "datasourceId": 12,
      "permissionLevel": "USE"
    }
  ]
}
```

Validation:

- target `ARCHIVE_ADMIN` may receive only `MANAGE`
- target `NORMAL_USER` may receive only `USE`
- archive administrators may submit only `USE` assignments
- every submitted datasource must be within the operator's grantable `MANAGE` scope

### Datasource APIs

#### List Datasources

`GET /api/v1/datasources`

Rules:

- `SYSTEM_ADMIN`: all datasources
- `ARCHIVE_ADMIN`: datasources in `MANAGE` scope
- `NORMAL_USER`: datasources in `USE` scope, with `MANAGE` also qualifying if present

#### Create Datasource

`POST /api/v1/datasources`

Rules:

- only `SYSTEM_ADMIN`

#### Update Or Disable Datasource

- `PUT /api/v1/datasources/{id}`
- `PUT /api/v1/datasources/{id}/status`
- `POST /api/v1/datasources/{id}/test`

Rules:

- `SYSTEM_ADMIN`: unrestricted
- `ARCHIVE_ADMIN`: allowed only when holding `MANAGE`
- `NORMAL_USER`: forbidden

### Archive Group APIs

#### List And Detail

- `GET /api/v1/archive-groups`
- `GET /api/v1/archive-groups/{id}`

Return only groups whose source and target datasource set is readable for the current user.

#### Create And Edit

- `POST /api/v1/archive-groups`
- `PUT /api/v1/archive-groups/{id}`

Validation:

- `SYSTEM_ADMIN`: any datasource pair
- `ARCHIVE_ADMIN`: both datasources must be in `MANAGE` scope
- `NORMAL_USER`: both datasources must be in `USE` scope, with `MANAGE` covering `USE`

#### Trigger And Cancel

- `POST /api/v1/archive-groups/{id}/trigger`
- `POST /api/v1/archive-groups/{id}/cancel`

Use the same datasource-scope rules as archive-group editability.

### Task APIs

- `GET /api/v1/tasks`
- `GET /api/v1/tasks/{id}`
- `GET /api/v1/tasks/{id}/logs`

All task read operations must derive authorization from the owning archive group's full datasource set.

## Frontend Design

### Auth Store

Replace a single `isAdmin` boolean with:

- `roleCode`
- `capabilities`
- helpers:
  - `isSystemAdmin`
  - `isArchiveAdmin`
  - `isNormalUser`
  - `hasCapability(capability)`

Reason:

- a three-role model cannot be represented safely by one admin boolean

### User Management UI

#### `SYSTEM_ADMIN`

Can:

- see user-management menu
- create users with role choices `ARCHIVE_ADMIN` and `NORMAL_USER`
- manage datasource authorizations for both archive administrators and normal users

#### `ARCHIVE_ADMIN`

Can:

- see user-management menu
- create only `NORMAL_USER`
- edit only normal users
- assign datasource `USE` authorizations only within grantable datasource scope

UI requirements:

- role selector locked to `NORMAL_USER`
- datasource authorization dialog must show only grantable datasources
- dialog actions must expose only `USE` assignment semantics

#### `NORMAL_USER`

Cannot:

- see user-management menu
- open user-management views

### Datasource UI

#### `SYSTEM_ADMIN`

Can:

- create datasource
- edit datasource
- disable datasource
- test datasource
- assign datasource management scope to archive administrators

#### `ARCHIVE_ADMIN`

Can:

- view only datasources in `MANAGE` scope
- edit, disable, and test only those datasources

Cannot:

- create datasource

#### `NORMAL_USER`

Recommended behavior:

- hide the standalone datasource-management menu
- expose selectable datasource lists only inside archive-business forms

### Archive Group And Task Forms

Datasource selectors must be fed by current-user selectable datasource scope.

Recommended selector behavior:

- `SYSTEM_ADMIN`: all datasources
- `ARCHIVE_ADMIN`: datasources in `MANAGE` scope
- `NORMAL_USER`: datasources in `USE` scope, with `MANAGE` also qualifying if present

Frontend filtering is only for UX. Backend validation remains mandatory.

## Error Code And Exception Design

This repository's governance rules require business-side failures to use project-defined exceptions and error codes.

Recommended new or expanded `StarterErrorCode` entries:

- `PERMISSION_DENIED`
- `USER_ROLE_INVALID_FOR_CREATOR`
- `USER_TARGET_ROLE_NOT_EDITABLE`
- `DATASOURCE_MANAGE_PERMISSION_REQUIRED`
- `DATASOURCE_USE_PERMISSION_REQUIRED`
- `DATASOURCE_AUTHORIZATION_OUT_OF_SCOPE`
- `DATASOURCE_AUTHORIZATION_LEVEL_INVALID`
- `ARCHIVE_GROUP_DATASOURCE_SCOPE_INVALID`
- `TASK_ACCESS_DENIED`
- `USER_ACCESS_DENIED`

Do not introduce raw `IllegalArgumentException` or `IllegalStateException` in service-layer authorization logic.

## Audit And Operation Log Design

The following actions should produce explicit operation logs:

- create user
- update user
- update user status
- assign datasource `MANAGE`
- assign datasource `USE`
- revoke datasource authorization
- replace datasource authorizations
- create datasource
- edit datasource
- disable datasource

Authorization-failure logging should be recorded where useful for investigation, especially for:

- archive administrator trying to assign out-of-scope datasource access
- normal user attempting to select unauthorized datasources
- unauthorized group or task access attempts

## Migration Strategy

Although the design does not preserve old behavior, implementation should still migrate usable existing data.

Recommended migration sequence:

1. create the new datasource-authorization table
2. normalize role values into the new three-role set
3. migrate existing datasource permissions into new authorization levels
4. introduce new authorization services
5. replace service and controller enforcement points
6. update frontend protocol and menus
7. remove old permission assumptions and stale role logic

### Role Migration

Recommended migration mapping:

- existing `platform_admin` -> `SYSTEM_ADMIN`
- existing `archive_admin` -> `ARCHIVE_ADMIN`
- existing `auditor` -> `NORMAL_USER`
- existing `observer` -> `NORMAL_USER`
- legacy `ADMIN` -> `SYSTEM_ADMIN`
- legacy `USER` -> `ARCHIVE_ADMIN` only if that legacy meaning still exists in stored data; otherwise normalize explicitly during migration SQL

### Datasource Permission Migration

Recommended mapping from old `READ` semantics:

- if target user role is `ARCHIVE_ADMIN`, migrate to `MANAGE`
- if target user role is `NORMAL_USER`, migrate to `USE`

Migration ownership notes:

- `granted_by_user_id` may use a bootstrap system-admin user when original grant operator cannot be reconstructed reliably
- `grant_source` should reflect whether migration is treated as system normalization or business-preserving seed logic

## Implementation Order

Recommended delivery order:

1. database migration and enum introduction
2. `CurrentUserService`, `RoleCapabilityService`, `DatasourceAuthorizationService`, and `ArchiveResourceAccessService`
3. user-management service and controller refactor
4. datasource authorization service and controller refactor
5. datasource service refactor
6. archive-group and task authorization refactor
7. frontend auth-store and page capability refactor
8. cleanup of old permission logic and stale role handling

## Test Strategy

### Unit Tests

Add focused tests for:

- `RoleCapabilityService`
- `DatasourceAuthorizationService`
- `ArchiveResourceAccessService`

Critical assertions:

- `SYSTEM_ADMIN` passes all checks
- `ARCHIVE_ADMIN` may assign only `USE`
- `ARCHIVE_ADMIN` may assign only within `MANAGE` scope
- `NORMAL_USER` cannot manage datasource or users
- `MANAGE` satisfies `USE`
- `USE` does not satisfy `MANAGE`

### Service Tests

Priority services:

- user service
- datasource authorization service
- datasource service
- archive-group service
- archive-group execution service
- task-log service if it does authorization-aware access

Critical scenarios:

- archive administrator creates normal user successfully
- archive administrator cannot create archive administrator
- archive administrator cannot grant non-managed datasource access
- normal user cannot edit datasource
- normal user cannot create or edit archive group with unauthorized datasource
- task read fails when either source or target datasource is out of scope

### Controller Contract Tests

Verify:

- unauthorized requests return permission failure
- authorized requests succeed
- list endpoints are filtered by datasource scope

### Frontend Tests

Verify:

- capability-driven menus render correctly by role
- user role selector is constrained correctly
- datasource authorization dialog shows only grantable datasources
- normal-user archive-group forms expose only selectable datasources

## Risks And Guardrails

### Source-Only Authorization Regression

Current logic is vulnerable because datasource access may be checked from only one side of a group or task.

Guardrail:

- every group and task access rule must validate both source and target datasource IDs

### Owner-Based Drift

Developers may accidentally continue using `owner_user_id` as permission truth.

Guardrail:

- document and enforce that owner fields are informational, not authorization truth

### Frontend-Only Restriction Drift

UI hiding without backend enforcement would create privilege-escalation risk.

Guardrail:

- all role and datasource-scope rules must be rechecked in backend services

### Over-Broad Archive Admin User Management

If archive administrators are allowed to edit too many users, the model becomes ambiguous and risky.

Guardrail:

- this design explicitly limits archive administrators to managing only `NORMAL_USER`
- archive administrators may not edit user roles

## Final Recommendation

Implement the refactor as a role-plus-datasource-authorization model with datasource scope as the only resource truth for archive-business access.

This design gives EasyArchive:

- a precise three-role platform model
- safe delegated datasource access
- consistent authorization for datasource, archive-group, and task operations
- auditable permission changes
- a clear path to future evolution without prematurely building a generic IAM platform
