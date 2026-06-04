# easyarchive-starter Management Transaction Design

## Background

`easyarchive-starter` currently exposes management-side service methods that update the platform management database without consistent Spring transaction boundaries. Most write methods perform more than one persistence step, such as:

- load current state
- update or delete the main record
- reload the updated record
- write an operation log entry

Without a service-layer transaction, these flows can leave the platform database in a partially applied state when any later step fails.

This design only covers management database writes inside `easyarchive-starter`. It does not change archive execution transactions in `easyarchive-core`, and it does not attempt to wrap external side effects such as datasource connectivity checks or async archive execution in one transaction.

## Goal

Add consistent transaction boundaries to management-side write service methods so that:

- platform data changes and corresponding operation logs commit together
- multi-step writes either fully succeed or fully roll back
- read-only service methods remain non-transactional
- transaction boundaries stay at the Spring service layer

## Scope

In scope:

- `easyarchive-starter/src/main/java/com/openquartz/easyarchive/starter/service/impl`
- methods that write platform management tables through MyBatis mappers
- methods that orchestrate multiple platform writes or write-plus-log sequences

Out of scope:

- controllers
- mapper interfaces and mapper XML
- `easyarchive-core`
- cross-thread async execution
- source database and target database archive transactions
- schema changes

## Transaction Rule

Use method-level `@Transactional(rollbackFor = Exception.class)` on write entrypoints in service implementations.

Rule set:

1. Add a transaction to any service method that calls platform-table `insert`, `update`, `delete`, or `batchInsert`.
2. Add a transaction to any service method that performs a read-modify-write-log flow, even if the write is a single mapper call.
3. Leave pure query methods unchanged.
4. Keep the boundary at the outer service method instead of moving it into mapper or helper methods.

Method-level annotations are preferred over class-level annotations because they keep read paths outside transactions and make write boundaries explicit.

## Service Coverage

### ArchiveConnectionServiceImpl

Add transactions to:

- `create`
- `update`
- `updateStatus`
- `testConnection`

Rationale:

- these methods write datasource records
- `update`, `updateStatus`, and `testConnection` depend on before/after snapshots
- operation logs should roll back with datasource state changes

`testConnection` should only make the platform-table update transactional. The network check itself remains an external side effect outside rollback guarantees.

### ArchiveGroupServiceImpl

Add transactions to:

- `create`
- `update`
- `updateStatus`
- `delete`

Rationale:

- all four methods modify archive group state
- the methods compute validations from current persisted state and then record operation logs

### ArchiveGroupItemByIdServiceImpl

Add transactions to:

- `create`
- `update`
- `updateStatus`
- `delete`

Rationale:

- all four methods update management tables
- uniqueness checks, state transitions, and operation log recording should share one transactional boundary

### ArchiveGroupItemByTimeServiceImpl

Add transactions to:

- `create`
- `update`
- `updateStatus`
- `delete`

Rationale matches the ID-based item service.

### UserServiceImpl

Add transactions to:

- `create`
- `update`
- `updateStatus`

Rationale:

- these methods write user records
- `update` and `updateStatus` use before/after snapshots for operation logs

### UserDatasourcePermissionServiceImpl

Keep the existing transaction on:

- `replacePermissions`

Add transactions to:

- `grant`
- `revoke`

Rationale:

- permission changes and operation log writes must commit together
- `replacePermissions` already shows this service owns a multi-step write flow

### ArchiveTaskLogServiceImpl

Keep existing transactional write methods unchanged unless a small consistency cleanup is needed for annotation style.

### ArchiveGroupExecutionServiceImpl

Add transactions to:

- `trigger`
- `cancelActiveTask`

Rationale:

- `trigger` inserts a platform task record before dispatching async execution
- `cancelActiveTask` changes platform task state through `ArchiveTaskLogService`

Boundary note:

- `trigger` should only protect the synchronous platform write path that creates the task record and prepares dispatch
- async archive execution remains outside this transaction
- rollback cannot undo side effects that happen after async dispatch starts

Because of that limitation, implementation should keep the transaction narrowly around the current synchronous path and avoid expanding it into background execution concerns.

## Read Methods

Do not add transactions to read-only methods such as:

- `findAll`
- `findById`
- `tree`
- `findOverview`
- `listUserPermissions`
- dashboard query methods
- auth query methods
- permission query methods

No class-level transaction should be introduced for these services because that would unnecessarily wrap read traffic.

## Rollback Policy

Use `rollbackFor = Exception.class` consistently.

This ensures rollback for failures such as:

- operation log persistence failure after the main row update
- partial success in delete-then-insert permission replacement
- failures in reloading after-state used for operation logs
- checked exceptions introduced later in service collaborators

## Testing Strategy

Add or extend service-layer tests in `easyarchive-starter/src/test/java/.../service/impl` to verify transactional intent at the Spring proxy layer.

Minimum checks:

- each targeted service write method is annotated with `@Transactional`
- existing transactional services keep their annotation
- read methods remain unannotated

Where current tests already cover a write method's behavior, prefer adding focused annotation assertions rather than introducing large integration tests for this refactor.

## Risks and Constraints

- Spring self-invocation is not a concern for this change because transactions are placed on public service entrypoints.
- `trigger` dispatches async work after inserting the task row, so rollback only covers failures before dispatch side effects escape the calling thread.
- This refactor improves consistency for platform database writes but does not provide distributed transaction guarantees across logs, background workers, or external systems.

## Implementation Outline

1. Identify all targeted write methods in `service/impl`.
2. Add `org.springframework.transaction.annotation.Transactional` imports where needed.
3. Annotate each targeted write method with `@Transactional(rollbackFor = Exception.class)`.
4. Normalize existing write-method annotations only where needed for consistency.
5. Add or update tests that assert annotation coverage for targeted methods and non-coverage for read methods.
6. Run focused Maven tests for the touched service test classes.
