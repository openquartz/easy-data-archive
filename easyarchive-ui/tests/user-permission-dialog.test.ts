import assert from "node:assert/strict";
import test from "node:test";

function buildAllowedPermissionLevels(operatorRole: string, targetRole: string): string[] {
  if (operatorRole === "platform_admin") return ["MANAGE", "USE"];
  if (operatorRole === "archive_admin") {
    if (targetRole === "normal_user") return ["USE"];
    return [];
  }
  return [];
}

test("archive admin can only assign USE to normal user", () => {
  assert.deepEqual(buildAllowedPermissionLevels("archive_admin", "normal_user"), ["USE"]);
});

test("platform admin can assign both levels", () => {
  assert.deepEqual(buildAllowedPermissionLevels("platform_admin", "normal_user"), ["MANAGE", "USE"]);
});

test("normal user cannot assign any level", () => {
  assert.deepEqual(buildAllowedPermissionLevels("normal_user", "normal_user"), []);
});
