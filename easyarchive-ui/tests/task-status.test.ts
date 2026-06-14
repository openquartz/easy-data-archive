import assert from "node:assert/strict";
import test from "node:test";

import { isTaskTerminalStatus } from "../src/utils/taskStatus";

test("treats finished task states as terminal", () => {
  assert.equal(isTaskTerminalStatus(2), true);
  assert.equal(isTaskTerminalStatus(3), true);
  assert.equal(isTaskTerminalStatus(5), true);
});

test("keeps waiting, running, and cancelling tasks refreshable", () => {
  assert.equal(isTaskTerminalStatus(0), false);
  assert.equal(isTaskTerminalStatus(1), false);
  assert.equal(isTaskTerminalStatus(4), false);
  assert.equal(isTaskTerminalStatus(undefined), false);
});
