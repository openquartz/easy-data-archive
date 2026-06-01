export interface StatusDictionaryItem {
  label: string;
  tone: "neutral" | "success" | "danger";
}

export const datasourceStatusDictionary: Record<number, StatusDictionaryItem> = {
  0: { label: "Disabled", tone: "neutral" },
  1: { label: "Enabled", tone: "success" }
};

export const userStatusDictionary: Record<number, StatusDictionaryItem> = {
  0: { label: "Disabled", tone: "neutral" },
  1: { label: "Enabled", tone: "success" }
};

const fallbackStatus: StatusDictionaryItem = { label: "Unknown", tone: "danger" };

export function getStatusLabel(dictionary: Record<number, StatusDictionaryItem>, status?: number): string {
  if (typeof status !== "number") {
    return fallbackStatus.label;
  }
  return dictionary[status]?.label || fallbackStatus.label;
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
