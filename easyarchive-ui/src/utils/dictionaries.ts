export interface StatusDictionaryItem {
  labelKey: string;
  tone: "neutral" | "success" | "danger";
}

import { translate } from "../i18n";

export const datasourceStatusDictionary: Record<number, StatusDictionaryItem> = {
  0: { labelKey: "status.untested", tone: "neutral" },
  1: { labelKey: "status.enabled", tone: "success" },
  2: { labelKey: "status.disabled", tone: "danger" }
};

export const userStatusDictionary: Record<number, StatusDictionaryItem> = {
  0: { labelKey: "status.enabled", tone: "success" },
  1: { labelKey: "status.disabled", tone: "neutral" }
};

export const archiveEnableStatusDictionary: Record<number, StatusDictionaryItem> = {
  0: { labelKey: "status.enabled", tone: "success" },
  1: { labelKey: "status.disabled", tone: "neutral" }
};

export const taskStatusDictionary: Record<number, StatusDictionaryItem> = {
  0: { labelKey: "task.status.waiting", tone: "neutral" },
  1: { labelKey: "task.status.running", tone: "success" },
  2: { labelKey: "task.status.success", tone: "success" },
  3: { labelKey: "task.status.failed", tone: "danger" },
  4: { labelKey: "task.status.cancelling", tone: "neutral" },
  5: { labelKey: "task.status.cancelled", tone: "danger" }
};

const fallbackStatus: StatusDictionaryItem = { labelKey: "common.unknown", tone: "danger" };

export function getStatusLabel(dictionary: Record<number, StatusDictionaryItem>, status?: number): string {
  if (typeof status !== "number") {
    return translate(fallbackStatus.labelKey);
  }
  return translate(dictionary[status]?.labelKey || fallbackStatus.labelKey);
}

export function getStatusTone(
  dictionary: Record<number, StatusDictionaryItem>,
  status?: number
): StatusDictionaryItem["tone"] {
  if (typeof status !== "number") {
    return fallbackStatus.tone;
  }
  return dictionary[status]?.tone || fallbackStatus.tone;
}

export function getStatusTagClass(
  dictionary: Record<number, StatusDictionaryItem>,
  status?: number
): string {
  return `status-tag status-tag--${getStatusTone(dictionary, status)}`;
}
