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

test("guide content includes operator examples for expression-driven scenarios", () => {
  const sections = buildGuideSections("zh-CN");
  const exampleSection = sections.find((section) => section.id === "examples-validation");
  const overviewSection = sections.find((section) => section.id === "rule-overview");

  assert.ok(exampleSection);
  assert.ok(overviewSection);
  assert.ok(
    exampleSection.items?.some(
      (item) => item.includes("最大 ID") && item.includes("{sql") && item.includes("max(id)")
    )
  );
  assert.ok(
    exampleSection.items?.some(
      (item) => item.includes("最小 ID") && item.includes("{sql") && item.includes("min(id)")
    )
  );
  assert.ok(
    exampleSection.items?.some(
      (item) => item.includes("目标表名") && item.includes("{hash_mod") && item.includes("按某个字段实际值进行 hash 并取模")
    )
  );
  assert.ok(
    exampleSection.items?.some(
      (item) => item.includes("来源表名") && item.includes("{time") && item.includes("_yyyyMMdd")
    )
  );
  assert.ok(
    overviewSection.items?.some(
      (item) => item.includes("{mod") && item.includes("{hash_mod")
    )
  );
});
