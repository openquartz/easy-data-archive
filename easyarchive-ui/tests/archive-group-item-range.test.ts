import test from "node:test";
import assert from "node:assert/strict";

import { formatArchiveGroupItemRange } from "../src/utils/archiveGroupItemRange.ts";

test("formats id item range as start and end boundary", () => {
  assert.equal(
    formatArchiveGroupItemRange({
      itemType: "ID",
      id: 1,
      groupId: 2,
      sourceTable: "sys_operation_log",
      targetTable: "sys_operation_log_archive",
      rangeStart: "0",
      rangeEnd: "1000",
      priority: 10
    }),
    "0 -> 1000"
  );
});

test("formats time item range as start and end timestamp", () => {
  assert.equal(
    formatArchiveGroupItemRange({
      itemType: "TIME",
      id: 2,
      groupId: 3,
      sourceTable: "sys_log",
      targetTable: "sys_log_archive",
      rangeStart: "2024-01-01 00:00:00",
      rangeEnd: "2024-02-01 00:00:00",
      priority: 20
    }),
    "2024-01-01 00:00:00 -> 2024-02-01 00:00:00"
  );
});

test("falls back to dash when range is incomplete", () => {
  assert.equal(
    formatArchiveGroupItemRange({
      itemType: "TIME",
      id: 2,
      groupId: 3,
      sourceTable: "sys_log",
      targetTable: "sys_log_archive",
      rangeStart: "2024-01-01 00:00:00",
      priority: 20
    }),
    "-"
  );
});
