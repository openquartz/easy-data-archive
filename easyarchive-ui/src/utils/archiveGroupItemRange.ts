import type { ArchiveGroupItemSummary } from "../api/archiveGroupItem";

export function formatArchiveGroupItemRange(item?: ArchiveGroupItemSummary | null): string {
  if (!item?.rangeStart || !item?.rangeEnd) {
    return "-";
  }
  return `${item.rangeStart} -> ${item.rangeEnd}`;
}
