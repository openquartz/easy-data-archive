import test from "node:test";
import assert from "node:assert/strict";

import {
  createArchiveGroupFormValue,
  createOwnerOptions,
  isNotificationConfigEditable,
  requiresWebhook
} from "../src/utils/archiveGroupForm";

test("createArchiveGroupFormValue preserves enabled notification config when notifyEnabled is a string", () => {
  const form = createArchiveGroupFormValue({
    id: 1,
    groupCode: "ORDER_ARCHIVE",
    groupName: "Order Archive",
    sourceDatasourceId: 10,
    targetDatasourceId: 11,
    enableStatus: 0,
    notifyEnabled: "1" as unknown as number,
    notifyChannel: "FEISHU",
    notifyWebhookUrl: "https://open.feishu.cn/hook/test"
  });

  assert.equal(form.notifyEnabled, 1);
  assert.equal(form.notifyChannel, "FEISHU");
  assert.equal(form.notifyWebhookUrl, "https://open.feishu.cn/hook/test");
  assert.equal(isNotificationConfigEditable(form), true);
});

test("createArchiveGroupFormValue enables editing for legacy notification config without explicit notifyEnabled", () => {
  const form = createArchiveGroupFormValue({
    id: 2,
    groupCode: "PAY_ARCHIVE",
    groupName: "Pay Archive",
    sourceDatasourceId: 20,
    targetDatasourceId: 21,
    enableStatus: 0,
    notifyChannel: "wecom" as unknown as "WECOM",
    notifyWebhookUrl: " https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test "
  });

  assert.equal(form.notifyEnabled, 1);
  assert.equal(form.notifyChannel, "WECOM");
  assert.equal(form.notifyWebhookUrl, "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=test");
  assert.equal(isNotificationConfigEditable(form), true);
});

test("createArchiveGroupFormValue keeps in-app channel without requiring webhook", () => {
  const form = createArchiveGroupFormValue({
    id: 3,
    groupCode: "IN_APP_ARCHIVE",
    groupName: "In App Archive",
    sourceDatasourceId: 30,
    targetDatasourceId: 31,
    enableStatus: 0,
    notifyEnabled: 1,
    notifyChannel: "IN_APP",
    notifyWebhookUrl: ""
  });

  assert.equal(form.notifyEnabled, 1);
  assert.equal(form.notifyChannel, "IN_APP");
  assert.equal(requiresWebhook(form), false);
});

test("createOwnerOptions includes current owner when user list is unavailable", () => {
  const options = createOwnerOptions([], {
    id: 3,
    groupCode: "IN_APP_ARCHIVE",
    groupName: "In App Archive",
    sourceDatasourceId: 30,
    targetDatasourceId: 31,
    ownerUserId: 4,
    ownerDisplayName: "jackchen (jackchen)",
    enableStatus: 0
  });

  assert.deepEqual(options, [
    {
      id: 4,
      username: "jackchen",
      realName: "jackchen",
      status: 0
    }
  ]);
});
