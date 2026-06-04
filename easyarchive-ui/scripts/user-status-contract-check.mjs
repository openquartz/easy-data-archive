import fs from "node:fs";
import path from "node:path";

const root = process.cwd();

function read(file) {
  return fs.readFileSync(path.join(root, file), "utf8");
}

function assertIncludes(source, fragment, message) {
  if (!source.includes(fragment)) {
    throw new Error(message);
  }
}

function assertNotIncludes(source, fragment, message) {
  if (source.includes(fragment)) {
    throw new Error(message);
  }
}

const dictionaries = read("src/utils/dictionaries.ts");
assertIncludes(
  dictionaries,
  '0: { labelKey: "status.enabled", tone: "success" }',
  "User status dictionary must treat 0 as enabled."
);
assertIncludes(
  dictionaries,
  '1: { labelKey: "status.disabled", tone: "neutral" }',
  "User status dictionary must treat 1 as disabled."
);

const userForm = read("src/components/UserFormDialog.vue");
assertIncludes(userForm, "status: 0,", "User form must default new users to enabled status 0.");
assertIncludes(userForm, '<option :value="0">{{ t("status.enabled") }}</option>', "User form must map 0 to enabled.");
assertIncludes(userForm, '<option :value="1">{{ t("status.disabled") }}</option>', "User form must map 1 to disabled.");

const userView = read("src/views/UserView.vue");
assertIncludes(
  userView,
  "const nextStatus = item.status === 0 ? 1 : 0;",
  "User status toggle must switch from enabled(0) to disabled(1)."
);
assertIncludes(
  userView,
  "{{ item.status === 0 ? t(\"common.disable\") : t(\"common.enable\") }}",
  "User action label must reflect 0=enabled and 1=disabled."
);

const layout = read("src/layouts/AppLayout.vue");
assertNotIncludes(layout, 't("layout.nav.login")', "Authenticated layout should not render a login navigation entry.");
assertIncludes(layout, 't("layout.actions.logout")', "Authenticated layout should expose a logout action.");
assertIncludes(
  layout,
  "router.push({ name: \"login\" })",
  "Logout flow should navigate back to the login page after clearing session state."
);

console.log("User status contract check passed.");
