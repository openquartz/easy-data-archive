import type { ArchiveGroupItemSummary } from "../api/archiveGroupItem";

export interface ArchiveGroupItemBuckets {
  timeItems: ArchiveGroupItemSummary[];
  idItems: ArchiveGroupItemSummary[];
}

export function splitArchiveGroupItemsByType(items: ArchiveGroupItemSummary[]): ArchiveGroupItemBuckets {
  return {
    timeItems: items.filter((item) => item.itemType === "TIME"),
    idItems: items.filter((item) => item.itemType === "ID")
  };
}
