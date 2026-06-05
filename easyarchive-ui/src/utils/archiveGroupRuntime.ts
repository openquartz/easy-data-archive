export interface ArchiveGroupRuntimeState {
  activeTaskId?: number;
  activeTaskStatus?: number;
  activeTaskProcessedRecords?: number;
  canTrigger?: boolean;
  canCancelActiveTask?: boolean;
  canViewActiveTask?: boolean;
}

const ARCHIVE_GROUP_RUNTIME_SUCCESS_PROGRESS = 100;
const ARCHIVE_GROUP_RUNTIME_CAPPED_PROGRESS = 95;
const ARCHIVE_GROUP_RUNTIME_LOG_BASELINE = 12;
const ARCHIVE_GROUP_RUNTIME_LOG_MULTIPLIER = 8;
const ARCHIVE_GROUP_RUNTIME_LOG_CAP = 83;
const ARCHIVE_GROUP_TASK_STATUS_RUNNING = 1;
const ARCHIVE_GROUP_TASK_STATUS_SUCCESS = 2;
const ARCHIVE_GROUP_TASK_STATUS_FAILED = 3;
const ARCHIVE_GROUP_TASK_STATUS_CANCELLING = 4;
const ARCHIVE_GROUP_TASK_STATUS_CANCELLED = 5;

export function getArchiveGroupRuntimeProcessedRecords(group?: ArchiveGroupRuntimeState | null): number {
  return typeof group?.activeTaskProcessedRecords === "number" && group.activeTaskProcessedRecords > 0
    ? group.activeTaskProcessedRecords
    : 0;
}

export function hasArchiveGroupActiveTask(group?: ArchiveGroupRuntimeState | null): boolean {
  return typeof group?.activeTaskId === "number" && group.activeTaskId > 0;
}

export function canTriggerArchiveGroup(group?: ArchiveGroupRuntimeState | null): boolean {
  return group?.canTrigger === true && !hasArchiveGroupActiveTask(group);
}

export function canViewArchiveGroupActiveTask(group?: ArchiveGroupRuntimeState | null): boolean {
  if (!hasArchiveGroupActiveTask(group)) {
    return false;
  }
  return group?.canViewActiveTask !== false;
}

export function canCancelArchiveGroupActiveTask(group?: ArchiveGroupRuntimeState | null): boolean {
  if (!hasArchiveGroupActiveTask(group)) {
    return false;
  }
  return group?.canCancelActiveTask === true;
}

function resolveArchiveGroupRuntimeSimulatedProgress(processedRecords: number): number {
  return Math.min(
    ARCHIVE_GROUP_RUNTIME_CAPPED_PROGRESS,
    ARCHIVE_GROUP_RUNTIME_LOG_BASELINE
      + Math.min(
        ARCHIVE_GROUP_RUNTIME_LOG_CAP,
        Math.floor(Math.log(processedRecords + 1) * ARCHIVE_GROUP_RUNTIME_LOG_MULTIPLIER)
      )
  );
}

export function resolveArchiveGroupRuntimeProgress(group?: ArchiveGroupRuntimeState | null): number {
  if (!hasArchiveGroupActiveTask(group)) {
    return 0;
  }

  if (group?.activeTaskStatus === ARCHIVE_GROUP_TASK_STATUS_SUCCESS) {
    return ARCHIVE_GROUP_RUNTIME_SUCCESS_PROGRESS;
  }

  if (group?.activeTaskStatus === ARCHIVE_GROUP_TASK_STATUS_CANCELLING) {
    return ARCHIVE_GROUP_RUNTIME_CAPPED_PROGRESS;
  }

  const processedRecords = getArchiveGroupRuntimeProcessedRecords(group);
  if (processedRecords <= 0) {
    return 0;
  }

  if (
    group?.activeTaskStatus === ARCHIVE_GROUP_TASK_STATUS_RUNNING
    || group?.activeTaskStatus === ARCHIVE_GROUP_TASK_STATUS_FAILED
    || group?.activeTaskStatus === ARCHIVE_GROUP_TASK_STATUS_CANCELLED
  ) {
    return resolveArchiveGroupRuntimeSimulatedProgress(processedRecords);
  }

  return 0;
}
