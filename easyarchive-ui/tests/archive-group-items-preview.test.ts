import test from "node:test";
import assert from "node:assert/strict";

import { splitArchiveGroupItemsByType } from "../src/utils/archiveGroupItemPreview";

test("splitArchiveGroupItemsByType groups summary items into time and id buckets", () => {
  const result = splitArchiveGroupItemsByType([
    { id: 1, itemType: "TIME", sourceTable: "orders", targetTable: "orders_hist", groupId: 1, priority: 10 },
    { id: 2, itemType: "ID", sourceTable: "pay", targetTable: "pay_hist", groupId: 1, priority: 20 }
  ]);

  assert.equal(result.timeItems.length, 1);
  assert.equal(result.idItems.length, 1);
  assert.equal(result.timeItems[0]?.id, 1);
  assert.equal(result.idItems[0]?.id, 2);
});

test("splitArchiveGroupItemsByType keeps empty buckets for empty input", () => {
  const result = splitArchiveGroupItemsByType([]);

  assert.deepEqual(result.timeItems, []);
  assert.deepEqual(result.idItems, []);
});
