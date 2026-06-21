export type PrimaryNavRouteName =
  | "dashboard"
  | "guide"
  | "datasources"
  | "archive-groups"
  | "tasks"
  | "operation-logs"
  | "users";

export type PrimaryNavItem = {
  routeName: PrimaryNavRouteName;
  labelKey: string;
  adminOnly?: boolean;
  capability?: string;
};

const PRIMARY_NAV_ITEMS: PrimaryNavItem[] = [
  { routeName: "dashboard", labelKey: "layout.nav.dashboard" },
  { routeName: "guide", labelKey: "layout.nav.guide" },
  { routeName: "datasources", labelKey: "layout.nav.datasources", capability: "DATASOURCE_VIEW_AUTHORIZED" },
  { routeName: "archive-groups", labelKey: "layout.nav.archiveGroups", capability: "ARCHIVE_GROUP_VIEW" },
  { routeName: "tasks", labelKey: "layout.nav.tasks" },
  { routeName: "operation-logs", labelKey: "layout.nav.operationLogs", adminOnly: true },
  { routeName: "users", labelKey: "layout.nav.users", adminOnly: true }
];

export function buildPrimaryNavItems(isAdmin: boolean, hasCapability?: (cap: string) => boolean): PrimaryNavItem[] {
  return PRIMARY_NAV_ITEMS.filter((item) => {
    if (item.adminOnly) {
      return isAdmin;
    }
    if (item.capability && hasCapability) {
      return hasCapability(item.capability);
    }
    return true;
  });
}
