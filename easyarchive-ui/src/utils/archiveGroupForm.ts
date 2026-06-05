import type { ArchiveGroup, ArchiveGroupPayload } from "../api/archiveGroup";

type ArchiveGroupSeed = Partial<ArchiveGroup> &
  Pick<
    ArchiveGroup,
    "groupCode" | "groupName" | "sourceDatasourceId" | "targetDatasourceId" | "enableStatus"
  >;

function normalizeNotifyEnabled(
  notifyEnabled: unknown,
  notifyChannel: ArchiveGroup["notifyChannel"],
  notifyWebhookUrl: ArchiveGroup["notifyWebhookUrl"]
): 0 | 1 {
  if (notifyEnabled === 1 || notifyEnabled === "1") {
    return 1;
  }
  if (notifyEnabled === 0 || notifyEnabled === "0") {
    return 0;
  }
  return notifyChannel || notifyWebhookUrl?.trim() ? 1 : 0;
}

function normalizeNotifyChannel(
  notifyChannel: ArchiveGroup["notifyChannel"]
): ArchiveGroupPayload["notifyChannel"] {
  if (typeof notifyChannel !== "string") {
    return undefined;
  }
  const normalized = notifyChannel.trim().toUpperCase();
  if (normalized === "FEISHU" || normalized === "WECOM") {
    return normalized;
  }
  return undefined;
}

export function createArchiveGroupFormValue(seed?: ArchiveGroupSeed | null): ArchiveGroupPayload {
  const notifyChannel = normalizeNotifyChannel(seed?.notifyChannel);
  const notifyWebhookUrl = seed?.notifyWebhookUrl?.trim() || "";
  const notifyEnabled = normalizeNotifyEnabled(seed?.notifyEnabled, notifyChannel, notifyWebhookUrl);

  return {
    parentId: seed?.parentId,
    groupCode: seed?.groupCode || "",
    groupName: seed?.groupName || "",
    groupPath: seed?.groupPath,
    groupLevel: seed?.groupLevel,
    sourceDatasourceId: seed?.sourceDatasourceId ?? 0,
    targetDatasourceId: seed?.targetDatasourceId ?? 0,
    ownerUserId: seed?.ownerUserId,
    enableStatus: seed?.enableStatus ?? 0,
    notifyEnabled,
    notifyChannel,
    notifyWebhookUrl,
    remark: seed?.remark || ""
  };
}

export function isNotificationConfigEditable(form: Pick<ArchiveGroupPayload, "notifyEnabled">): boolean {
  return form.notifyEnabled === 1;
}
