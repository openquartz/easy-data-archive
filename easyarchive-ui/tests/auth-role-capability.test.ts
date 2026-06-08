import assert from "node:assert/strict";
import test from "node:test";

import { normalizeRoleCode } from "../src/constants/roles";

test("maps archive admin correctly", () => {
  assert.equal(normalizeRoleCode("ARCHIVE_ADMIN"), "archive_admin");
});

test("maps normal user correctly", () => {
  assert.equal(normalizeRoleCode("NORMAL_USER"), "normal_user");
});

test("defaults to normal_user for unknown", () => {
  assert.equal(normalizeRoleCode("UNKNOWN"), "normal_user");
});

test("handles legacy ADMIN", () => {
  assert.equal(normalizeRoleCode("ADMIN"), "platform_admin");
});

test("handles legacy USER", () => {
  assert.equal(normalizeRoleCode("USER"), "archive_admin");
});
