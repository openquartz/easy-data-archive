import test from "node:test";
import assert from "node:assert/strict";

import {
  canCancelArchiveGroupActiveTask,
  canTriggerArchiveGroup,
  canViewArchiveGroupActiveTask,
  getArchiveGroupRuntimeProcessedRecords,
  hasArchiveGroupActiveTask,
  resolveArchiveGroupRuntimeProgress
} from "../src/utils/archiveGroupRuntime.ts";

test("archive group without active task can be triggered when backend allows it", () => {
  const group = {
    id: 1,
    canTrigger: true
  };

  assert.equal(hasArchiveGroupActiveTask(group), false);
  assert.equal(canTriggerArchiveGroup(group), true);
  assert.equal(canViewArchiveGroupActiveTask(group), false);
  assert.equal(canCancelArchiveGroupActiveTask(group), false);
});

test("archive group with active task suppresses trigger and enables task actions", () => {
  const group = {
    id: 2,
    activeTaskId: 88,
    activeTaskStatus: 1,
    canTrigger: false,
    canCancelActiveTask: true,
    canViewActiveTask: true
  };

  assert.equal(hasArchiveGroupActiveTask(group), true);
  assert.equal(canTriggerArchiveGroup(group), false);
  assert.equal(canViewArchiveGroupActiveTask(group), true);
  assert.equal(canCancelArchiveGroupActiveTask(group), true);
});

test("running archive group with processed records exposes migrated count and simulated progress", () => {
  const group = {
    id: 3,
    activeTaskId: 99,
    activeTaskStatus: 1,
    activeTaskProcessedRecords: 1000
  };

  assert.equal(getArchiveGroupRuntimeProcessedRecords(group), 1000);
  assert.equal(resolveArchiveGroupRuntimeProgress(group), 67);
});

test("cancelling archive group is capped at 95 percent", () => {
  const group = {
    id: 4,
    activeTaskId: 100,
    activeTaskStatus: 4,
    activeTaskProcessedRecords: 200
  };

  assert.equal(resolveArchiveGroupRuntimeProgress(group), 95);
});

test("successful archive group reports full progress", () => {
  const group = {
    id: 5,
    activeTaskId: 101,
    activeTaskStatus: 2,
    activeTaskProcessedRecords: 1
  };

  assert.equal(resolveArchiveGroupRuntimeProgress(group), 100);
});
