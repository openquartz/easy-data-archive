import assert from "node:assert/strict";
import test from "node:test";

import {
  formatTaskLogConsoleMeta,
  getTaskLogConsoleLevelTone
} from "../src/utils/taskLogConsole";

test("maps task log levels to console tones", () => {
  assert.equal(getTaskLogConsoleLevelTone("INFO"), "success");
  assert.equal(getTaskLogConsoleLevelTone("warn"), "warning");
  assert.equal(getTaskLogConsoleLevelTone("ERROR"), "danger");
  assert.equal(getTaskLogConsoleLevelTone("DEBUG"), "neutral");
  assert.equal(getTaskLogConsoleLevelTone(undefined), "neutral");
});

test("formats console meta segments without empty placeholders", () => {
  assert.equal(formatTaskLogConsoleMeta("START", "LOAD_DATA"), "START / LOAD_DATA");
  assert.equal(formatTaskLogConsoleMeta("ERROR", undefined), "ERROR");
  assert.equal(formatTaskLogConsoleMeta(undefined, "TASK_END"), "TASK_END");
  assert.equal(formatTaskLogConsoleMeta(undefined, undefined), "-");
});
