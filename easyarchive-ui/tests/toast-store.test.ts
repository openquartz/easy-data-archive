import test from "node:test";
import assert from "node:assert/strict";

import {
  clearToasts,
  getToastSnapshot,
  showErrorToast,
  showSuccessToast
} from "../src/stores/toast";

test("toast store appends toasts and allows clearing", () => {
  clearToasts();

  const successId = showSuccessToast("Saved", 5000);
  const errorId = showErrorToast("Request failed", 5000);
  const snapshot = getToastSnapshot();

  assert.equal(snapshot.length, 2);
  assert.equal(snapshot[0]?.id, successId);
  assert.equal(snapshot[0]?.type, "success");
  assert.equal(snapshot[1]?.id, errorId);
  assert.equal(snapshot[1]?.type, "error");

  clearToasts();
  assert.deepEqual(getToastSnapshot(), []);
});

test("toast store auto dismisses after the configured duration", async () => {
  clearToasts();

  showErrorToast("Transient error", 10);
  assert.equal(getToastSnapshot().length, 1);

  await new Promise((resolve) => setTimeout(resolve, 30));

  assert.deepEqual(getToastSnapshot(), []);
});
