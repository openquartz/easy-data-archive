import assert from "node:assert/strict";
import test from "node:test";

import { messages } from "../src/i18n/messages.ts";

test("datasource table exposes a masked password column", () => {
  assert.equal(messages["en-US"].datasource.columns.password, "Password");
  assert.equal(messages["zh-CN"].datasource.columns.password, "密码");
});
