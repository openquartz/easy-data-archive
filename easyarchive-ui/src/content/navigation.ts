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
};

const PRIMARY_NAV_ITEMS: PrimaryNavItem[] = [
  { routeName: "dashboard", labelKey: "layout.nav.dashboard" },
  { routeName: "guide", labelKey: "layout.nav.guide" },
  { routeName: "datasources", labelKey: "layout.nav.datasources" },
  { routeName: "archive-groups", labelKey: "layout.nav.archiveGroups" },
  { routeName: "tasks", labelKey: "layout.nav.tasks" },
  { routeName: "operation-logs", labelKey: "layout.nav.operationLogs", adminOnly: true },
  { routeName: "users", labelKey: "layout.nav.users", adminOnly: true }
];

export function buildPrimaryNavItems(isAdmin: boolean): PrimaryNavItem[] {
  return PRIMARY_NAV_ITEMS.filter((item) => !item.adminOnly || isAdmin);
}
