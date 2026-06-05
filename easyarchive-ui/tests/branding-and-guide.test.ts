import assert from "node:assert/strict";
import test from "node:test";

import { messages } from "../src/i18n/messages.ts";
import { buildGuideSections } from "../src/content/guideContent.ts";
import { buildPrimaryNavItems } from "../src/content/navigation.ts";

test("brand copy uses data archive platform naming across visible entry points", () => {
  assert.equal(messages["zh-CN"].layout.brand, "数据归档平台");
  assert.equal(messages["zh-CN"].login.title, "数据归档平台");
  assert.equal(messages["en-US"].layout.brand, "Data Archive Platform");
  assert.equal(messages["en-US"].login.title, "Data Archive Platform");
});

test("sidebar puts guide immediately after dashboard for both admin and regular users", () => {
  assert.deepEqual(
    buildPrimaryNavItems(false).map((item) => item.routeName),
    ["dashboard", "guide", "datasources", "archive-groups", "tasks"]
  );
  assert.deepEqual(
    buildPrimaryNavItems(true).map((item) => item.routeName),
    ["dashboard", "guide", "datasources", "archive-groups", "tasks", "operation-logs", "users"]
  );
});

test("guide content exposes technical-manual sections and rollout safety guidance", () => {
  const sections = buildGuideSections("zh-CN");

  assert.deepEqual(
    sections.map((section) => section.id),
    [
      "positioning",
      "preparation",
      "datasource-config",
      "group-config",
      "rule-overview",
      "id-rule",
      "time-rule",
      "query-cleanup",
      "examples-validation",
      "troubleshooting"
    ]
  );

  assert.ok(
    sections.some((section) =>
      section.notes?.some((note) => note.includes("先开启写入，关闭清理"))
    )
  );
  assert.ok(
    sections.some((section) =>
      section.items?.some((item) => item.includes("删除条件"))
    )
  );
});
