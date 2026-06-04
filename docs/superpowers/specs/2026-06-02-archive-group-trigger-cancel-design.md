# Archive Group Trigger And Cancel Full-Chain Design

## Goal

Implement the full management chain for archive group manual trigger and archive task cancellation from UI to backend execution.

This scope includes:

- Manual trigger for one archive group at a time
- Group-page visibility of the current active task
- Cancel entry points from archive group page, task list page, and task detail page
- Consistent task state transitions and polling-based UI refresh

This scope excludes:

- Batch trigger or batch cancel
- Scheduled trigger
- Runtime parameterized trigger
- WebSocket or SSE push updates
- Forceful thread interruption

## Current State

The repository already contains most of the execution backbone:

- `ArchiveGroupExecutionServiceImpl.trigger(Long groupId)` creates a waiting task and dispatches execution
- `ArchiveTaskLogServiceImpl.cancelTask(Long taskId, String cancelReason)` marks tasks as `CANCELLING` or `CANCELLED`
- `ArchiveExecutor.checkCancellation()` polls task status from the repository
- `ArchiveGroupExecutor` catches `TaskCancelledException` and publishes a cancelled `TaskEndEvent`
- `ArchiveGroupView.vue` already exposes a trigger button
- `TaskDetailView.vue` already exposes a cancel button

What is still missing is a stable full-chain management flow:

- The group list does not expose active-task view fields
- The group page cannot cancel the current active task directly
- The task list page cannot cancel active tasks
- Group-level actions do not consistently reflect active-task state
- Backend group queries do not aggregate task runtime state for the UI

## Recommended Approach

Use a backend-aggregated group runtime view rather than letting the frontend derive active-task state from multiple APIs.

The backend remains the source of truth for:

- Whether a group can be triggered
- Whether a group currently has an active task
- Which task is the current active task
- Whether that active task can be cancelled

This keeps status rules in one place and prevents the frontend from guessing based on partial task lists.

## State Model

The task state machine remains:

- `WAITING(0)`
- `RUNNING(1)`
- `SUCCESS(2)`
- `FAILED(3)`
- `CANCELLING(4)`
- `CANCELLED(5)`

For this feature, an active task is the latest non-terminal task for one group, where status is one of:

- `WAITING`
- `RUNNING`
- `CANCELLING`

Key rules:

- One group can only have one active task at a time
- A group with an active task cannot be triggered again
- Cancelling a `WAITING` task moves directly to `CANCELLED`
- Cancelling a `RUNNING` task moves to `CANCELLING`, then the executor promotes it to `CANCELLED`
- Cancelling a `CANCELLING` task is idempotent
- Cancel means cooperative stop at the next checkpoint, not thread kill and not rollback

## Backend Design

### Group Runtime View

Introduce a UI-facing DTO such as `ArchiveGroupView` that wraps archive group data and active-task runtime fields:

- group fields copied from `ArchiveGroup`
- `activeTaskId`
- `activeTaskStatus`
- `activeTaskStartTime`
- `canTrigger`
- `canCancelActiveTask`
- `canViewActiveTask`

The backend computes these flags from:

- group enabled status
- enabled child item count
- datasource availability
- latest active task for the group

### APIs

Keep the existing command APIs:

- `POST /api/v1/archive/groups/{groupId}/trigger`
- `POST /api/v1/task-log/tasks/{taskId}/cancel`

Add one convenience command API for the archive group page:

- `POST /api/v1/archive/groups/{groupId}/cancel-active-task`

Add or adapt group query APIs to return the runtime view DTO:

- `GET /api/v1/archive/groups`
- `GET /api/v1/archive/groups/{groupId}`

### Service Responsibilities

`ArchiveGroupService`

- Returns aggregated group view models for list/detail reads
- Prevents destructive group operations while an active task exists

`ArchiveGroupExecutionService`

- Triggers a group when no active task exists
- Locates and cancels the current active task for a group

`ArchiveTaskLogService`

- Remains the task-level cancel entry point
- Owns task state transition validation and cancel log creation

### Data Access

Add mapper support to retrieve the latest active task for one group:

- `selectLatestActiveByGroupId(groupId)`

If list-page query cost later becomes a problem, add a batch query. It is not required for this phase.

## Frontend Design

### Archive Group Page

The group page becomes the main operational page for manual trigger management.

Each group row shows:

- group name and code
- datasource summary
- enable status
- active-task status if present
- active-task start time if present

Action rules:

- Show `Trigger` when `canTrigger` is true
- Show `Cancel Task` when `activeTaskId` exists and `canCancelActiveTask` is true
- Show `View Task` when `activeTaskId` exists

When an active task exists, disable group actions that could conflict with execution:

- disable delete
- disable status toggle
- disable edit in this phase

The page should poll every 5 seconds only while at least one active task exists in the current list.

### Task List Page

Add cancel action to each active task row:

- allow cancel when status is `WAITING`, `RUNNING`, or `CANCELLING`
- render cancelling state distinctly and avoid noisy repeated submits

### Task Detail Page

Keep the existing cancel entry but align state handling with the shared rule set:

- `WAITING`, `RUNNING`, `CANCELLING` are treated as cancellable display states
- `CANCELLING` should render as in-progress cancel, not as a fresh cancel action

## Error Handling And UX Rules

Return business errors for:

- group already has an active task
- no enabled archive items in the group
- group is disabled
- no current active task found for group cancellation
- task already ended

The frontend should present user-facing messages instead of raw backend exception text where possible.

## Testing Strategy

Add or update tests for:

- group service aggregation and group-operation restrictions
- group execution service `cancelActiveTask`
- task cancel idempotency and state transitions
- controller contracts for group list view fields and group cancel-active-task API
- frontend archive-group and task page action visibility and API usage

## Implementation Notes

- Keep execution cancellation cooperative and database-driven
- Do not add `Future.cancel(true)` or thread interruption in this scope
- Prefer small DTO and mapper additions over rewriting core entities
