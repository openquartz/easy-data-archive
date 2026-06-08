export const ROLE_CODES = ["platform_admin", "archive_admin", "normal_user"] as const;

export type RoleCode = (typeof ROLE_CODES)[number];

const LEGACY_ROLE_MAP: Record<string, RoleCode> = {
  ADMIN: "platform_admin",
  USER: "archive_admin"
};

export const DEFAULT_ROLE_CODE: RoleCode = "normal_user";

export function normalizeRoleCode(roleCode?: string | null): RoleCode {
  if (!roleCode) {
    return DEFAULT_ROLE_CODE;
  }
  const trimmed = roleCode.trim();
  if (!trimmed) {
    return DEFAULT_ROLE_CODE;
  }
  if (trimmed in LEGACY_ROLE_MAP) {
    return LEGACY_ROLE_MAP[trimmed];
  }
  const lowered = trimmed.toLowerCase();
  return (ROLE_CODES as readonly string[]).includes(lowered) ? (lowered as RoleCode) : DEFAULT_ROLE_CODE;
}

export function isAdminRole(roleCode?: string | null): boolean {
  return normalizeRoleCode(roleCode) === "platform_admin";
}
